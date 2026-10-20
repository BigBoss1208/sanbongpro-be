"""Real PostgreSQL baseline verification; never accepts a remote database URL.
Usage: python scripts/test_neon_baseline.py --pg-bin PATH --maven PATH
Creates a new SCRAM-authenticated cluster in target and always stops that cluster.
Does NOT connect to Neon/VPS, change application ddl-auto or run any deployment.
"""
from pathlib import Path
import argparse, json, os, secrets, socket, subprocess, uuid

parser=argparse.ArgumentParser()
parser.add_argument('--pg-bin',required=True)
parser.add_argument('--maven',required=True)
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
pg=Path(args.pg_bin).resolve()
exe=lambda name: str(pg/(name+('.exe' if os.name=='nt' else '')))
runid=uuid.uuid4().hex[:12]
work=root/'target'/('baseline-pg-'+runid)
work.mkdir(parents=True,exist_ok=False)
database='qlsb_baseline_test_'+runid
user='qlsb_baseline_test'
password=secrets.token_urlsafe(32)
pwfile=work/'init-password.tmp'
pwfile.write_text(password,encoding='utf-8')
env=os.environ.copy()
# Explicitly remove inherited connection/service overrides; all DB operations
# below pass loopback host, ephemeral port, generated database and test user.
for key in list(env):
    if key.startswith('PG') or key.startswith('SEED_ADMIN_'): env.pop(key)
env.update(PGPASSWORD=password,PGSSLMODE='disable',BASELINE_TEST_USER=user,BASELINE_TEST_PASSWORD=password)
with socket.socket() as sock:
    sock.bind(('127.0.0.1',0));port=sock.getsockname()[1]

def run(command,label,expected=0):
    log=work/(label+'.log')
    # File output prevents pg_ctl's background Windows children from keeping
    # an inherited stdout PIPE open and hanging subprocess.communicate().
    with log.open('wb') as stream:
        result=subprocess.run(command,cwd=root,env=env,stdout=stream,stderr=subprocess.STDOUT,timeout=180)
    output=log.read_bytes()
    # Only isolated fixtures appear in logs, never production credentials.
    log.write_bytes(output.replace(password.encode(),b'[TEST_PASSWORD_REDACTED]'))
    if (expected==0 and result.returncode!=0) or (expected!=0 and result.returncode==0):
        raise RuntimeError(label+' unexpected exit; inspect ignored log '+str(log))
    return output.decode('utf-8',errors='replace')

def sqlfile(path,label,expected=0):
    return run([exe('psql'),'-X','-h','127.0.0.1','-p',str(port),'-U',user,'-d',database,'-v','ON_ERROR_STOP=1','-f',str(root/path)],label,expected)

def query(sql,label):
    return run([exe('psql'),'-X','-h','127.0.0.1','-p',str(port),'-U',user,'-d',database,'-A','-t','-v','ON_ERROR_STOP=1','-c',sql],label).strip()

started=False
try:
    run([exe('initdb'),'-D',str(work/'data'),'-U',user,'--pwfile',str(pwfile),'--auth-local=scram-sha-256','--auth-host=scram-sha-256','--encoding=UTF8','--locale=C'],'initdb')
    pwfile.unlink()
    # pg_ctl starts PostgreSQL in the background without a visible terminal.
    started=True
    run([exe('pg_ctl'),'-D',str(work/'data'),'-l',str(work/'server.log'),'-o',f'-h 127.0.0.1 -p {port}','-w','start'],'start')
    run([exe('createdb'),'-h','127.0.0.1','-p',str(port),'-U',user,database],'createdb')
    sqlfile(Path('database/baseline/neon_schema.sql'),'baseline')
    indexes=query("select count(*) from pg_indexes where schemaname='public'",'indexes-before')
    for name in ['20261012_booking_payment_safety.sql','20261013_add_thanh_toan_refund_flag.sql']:
        sqlfile(Path('database/migrations')/name,'migration-'+name)
    assert query("select count(*) from pg_indexes where schemaname='public'",'indexes-after')==indexes,'Migration added duplicate index'
    sqlfile(Path('database/baseline/neon_schema.sql'),'baseline-repeat-rejected',expected=1)
    assert query("select count(*) from pg_tables where schemaname='public'",'tables-after-repeat')=='9'
    sqlfile(Path('database/baseline/neon_seed.sql'),'seed-missing-input-rejected',expected=1)
    env.update(SEED_ADMIN_USERNAME='baseline-admin',SEED_ADMIN_EMAIL='baseline-admin@example.invalid',SEED_ADMIN_NAME='Isolated admin',SEED_ADMIN_PASSWORD_HASH='invalid-test-hash')
    sqlfile(Path('database/baseline/neon_seed.sql'),'seed-invalid-hash-rejected',expected=1)
    assert query('select count(*) from public.nguoi_dung','users-after-invalid-seed')=='0'
    hashfile=work/'seed-hash.tmp'
    jdbc=f'jdbc:postgresql://127.0.0.1:{port}/{database}'
    run([args.maven,'-o','-Dspring.profiles.active=test','-Dtest=PostgresBaselineTest',f'-Dbaseline.jdbc.url={jdbc}',f'-Dbaseline.seed.hash.file={hashfile}','test'],'hibernate-validate')
    env.update(SEED_ADMIN_USERNAME='baseline-admin',SEED_ADMIN_EMAIL='baseline-admin@example.invalid',SEED_ADMIN_NAME='Isolated admin',SEED_ADMIN_PASSWORD_HASH=hashfile.read_text())
    sqlfile(Path('database/baseline/neon_seed.sql'),'seed')
    sqlfile(Path('database/baseline/neon_seed.sql'),'seed-repeat-rejected',expected=1)
    counts=query("select (select count(*) from nguoi_dung),(select count(*) from cum_san),(select count(*) from san),(select count(*) from khung_gio),(select count(*) from gia_san),(select count(*) from dat_san),(select count(*) from thanh_toan)",'seed-counts')
    assert counts=='1|1|1|3|3|0|0',counts
    for name in ['20261012_booking_payment_safety.sql','20261013_add_thanh_toan_refund_flag.sql']:
        sqlfile(Path('database/migrations')/name,'after-seed-'+name)
    assert query("select count(*) from pg_indexes where schemaname='public'",'final-indexes')==indexes
    assert query("select (select count(*) from nguoi_dung),(select count(*) from cum_san),(select count(*) from san),(select count(*) from khung_gio),(select count(*) from gia_san),(select count(*) from dat_san),(select count(*) from thanh_toan)",'seed-counts-after-migrations')==counts
    assert query("select role || '|' || khoa_tai_khoan::text from nguoi_dung",'seed-admin-state')=='ADMIN|false'
    assert query("select count(*) from thanh_toan where can_xu_ly_hoan_tien is null",'refund-null')=='0'
    print('PASS: PostgreSQL baseline, both migrations (no duplicate indexes), Hibernate validate, converter/enums round-trip, seed inputs/counts/repeat protection.')
    (work/'result.json').write_text(json.dumps({'status':'PASS','database':database,'host':'127.0.0.1','port':port,'index_count':int(indexes),'seed_counts':counts},indent=2))
finally:
    if started: run([exe('pg_ctl'),'-D',str(work/'data'),'-m','fast','-w','stop'],'stop')
    for sensitive in [pwfile,work/'seed-hash.tmp']:
        if sensitive.exists():sensitive.unlink()
    print('Temporary cluster stopped; artifacts retained only in ignored '+str(work.relative_to(root)))
