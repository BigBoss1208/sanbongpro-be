# Missing ThanhToan refund-handling column

Entity `ThanhToan.canXuLyHoanTien` maps Java `boolean` to `can_xu_ly_hoan_tien`. Read-only inspection of the PostgreSQL connection saved in IntelliJ confirms `public.thanh_toan` exists and this column is absent.

The accompanying SQL adds `BOOLEAN NOT NULL DEFAULT FALSE`. Existing rows remain present and receive false for the new flag, matching the Java field's initial value. Existing columns, payments, references and indexes are not modified. `IF NOT EXISTS` allows the same migration to be retried after a successful run without resetting flags. The transaction and lock/statement timeouts limit accidental long blocking.

This is a manual migration stored outside Spring resources. There is no automatic migration runner. Do not run it against the real database until the owner explicitly approves. Review and take a database backup before the approved deployment. Keep `spring.jpa.hibernate.ddl-auto=validate`; do not work around the missing column with `update` or `create`.

The older `20261012_booking_payment_safety.sql` also adds this column but includes a VNPay unique-index operation. Use this dedicated file for the currently requested column-only repair; the older file is unchanged.

After the approved migration, inspect `information_schema.columns` for `public.thanh_toan.can_xu_ly_hoan_tien`: type boolean, nullable NO and default false. Then restart the PostgreSQL-backed application with ddl-auto=validate and verify its health/API. Actual database startup cannot be reported as repaired while this migration remains unapplied.

Other observation: PostgreSQL `tong_tien` is numeric(19,2), while the entity declares precision 12/scale 0. This migration deliberately leaves it unchanged; changing amounts or precision is outside the requested column repair.

## Verification result (13 October 2026)

- Real PostgreSQL metadata inspected with `default_transaction_read_only=on`; missing column confirmed in public.thanh_toan. No transaction/customer/payment records were printed.
- Real PostgreSQL startup reproduced with explicit `ddl-auto=validate`, a read-only datasource, demo seeding disabled and an ephemeral loopback port: `Schema-validation: missing column [can_xu_ly_hoan_tien] in table [thanh_toan]`. It failed before startup completed. No real-database DDL or migration was executed.
- Backend `mvn -o test` passed 41 tests, zero failures/errors/skips. The added isolated H2 PostgreSQL-mode test verifies core ALTER TABLE syntax, old rows/amounts/references preserved, false default, non-null flag and repeat execution preserving a true flag. PostgreSQL-specific timeout statements have not been executed/validated on a staging PostgreSQL server.
- Application configuration, Entity ThanhToan and VNPay source remain unchanged. Existing local changes in .env.example, package-lock.json and the Frontend checkout were preserved and are outside this repair.
- Owner confirmation is still required before applying this SQL to the real PostgreSQL connection saved in IntelliJ. After approval, startup and API must be checked again; successful PostgreSQL startup is not yet claimed.

## Final-approval preflight

Read-only queries confirmed current_database()=ra99_n2 and current_schema()=public. public.thanh_toan has 3 rows and references 3 dat_san rows; no refund flag column exists. A data-only snapshot backup of these rows and their column/constraint/index/sequence metadata was captured using one REPEATABLE READ READ ONLY transaction. CSV counts/references, SHA-256 manifest and ZIP CRC were checked. Backup is local in database/backups (Git ignored, owner/SYSTEM access); no sensitive row contents or credentials are in this report. This is a CSV/metadata backup, not a full pg_dump archive. Restore helpers have not been executed. The production migration is still unapplied and final owner approval is pending.

## Conditional execution approval — precondition not yet satisfied

The owner authorized this exact file only after backup restorability and absence of processing payments are confirmed. Backup SHA-256 checks passed. Restoration into isolated H2 PostgreSQL mode recovered all 3 payment rows and 3 linked booking rows; all 54 field values, NULL/empty-string distinctions, row counts and payment-to-booking foreign key were verified. No restore was performed on real PostgreSQL; this remains a data-only backup rather than a complete PostgreSQL dump.

Read-only preflight confirmed ra99_n2/public, 3 payment rows and **3 CHO_THANH_TOAN rows**. Other write-lock holders on payment/booking tables and other observed payment-related transactions both counted zero. Pending status alone does not establish that no external payment flow is in progress. Execution therefore stopped before migration, awaiting clarification about these pending payments. No payment status, real database schema or other table was modified. PostgreSQL-backed Backend restart and /api/san verification are deferred because the migration has not been applied.

## Execution after the owner's subsequent explicit approval

The owner subsequently explicitly approved executing this exact migration on ra99_n2/public without changing payment data or other tables. Backup manifest/archive SHA-256 and ZIP CRC were rechecked, and the isolated restore PASS marker confirmed. Immediately before execution, all backed-up rows were compared with the real database using read-only queries; target database/schema and the 3 payment/3 related booking counts matched, with no other write-lock holder observed.

The SQL file was checked against the full reviewed text, then executed unchanged, including its BEGIN/timeouts/ALTER/COMMIT. It committed successfully. Post-execution metadata confirmed `boolean`, nullable `NO`, default `false`. All 3 payments remained, all 3 new flags were false, and all original payment fields and related booking fields matched the immediately-before snapshots. All 3 `CHO_THANH_TOAN` statuses were preserved. No other database mutation was executed.

Backend was then started against the real PostgreSQL settings with explicit `ddl-auto=validate`, generate-ddl=false and demo seeding disabled. Hibernate initialized the JPA EntityManager successfully: the missing-column error was resolved. Startup then failed on a new configuration error: `Could not resolve placeholder 'MINIO_ENDPOINT' in value "${MINIO_ENDPOINT}"`. No Tomcat 8084/application startup completed, so `/api/san` could not be verified. Work stopped at that error as instructed; no additional database, VNPay or environment repair was attempted.
