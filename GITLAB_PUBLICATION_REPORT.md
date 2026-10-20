# GitLab publication audit — 13 October 2026

**Publication stopped: confirmed credentials in Backend Git history. No commit, branch switch, merge, history rewrite or push was performed in this audit.**

## Repositories and branches

- Backend origin: https://gitlab.com/maiiilinhhh220204-group/quanlysanbong/be.git. Current branch update-backend-0926, HEAD f1306eea4cc49918c76493c55c35023039262090. GitLab main exists at e629b2a. Current HEAD is 7 commits ahead and 3 behind origin/main; current feature branch is synchronized with its own remote.
- Frontend origin: https://gitlab.com/maiiilinhhh220204-group/quanlysanbong/fe.git. Current branch codex/full-fix-20261012, HEAD ac731931f8b11ca85afc0f9ae3934db5456dd89f. GitLab main exists at 283deaf. Current HEAD is 4 ahead and 0 behind origin/main; current feature branch is synchronized with its own remote. Frontend working tree is clean.
- No remote URL changed. No currently staged or unmerged files. Working/staged diff checks passed. Synchronization/merge into main was deferred at the security gate; future merge conflicts have not been evaluated/applied.

## Security finding

Historical src/main/resources/application.yml contains literal database password, MinIO access/secret keys and VNPay hash secret. Confirmed example commits include d88e953 and 9aa92cf, both reachable from GitLab Backend origin/main. A historical database password matches the currently configured IntelliJ database credential. Values are intentionally omitted from all tool outputs and this report. Treat the historical credentials as exposed and rotate/revoke them; agree a remediation approach with the repository owner before publication. This audit did not rewrite history or change any credential.

Initial scanner candidates in Java/React password fields were variable references rather than embedded passwords. Masked placeholder entries in .env.example were not reported as confirmed live secrets. No tracked .env, IDE/build/dependency output or backup data file was found at current HEAD. Ignore rules cover .env variants, .idea, target, node_modules, dist, logs and local database backups. Backups remain private and ignored. This finding is sufficient to stop all project pushes under the owner's instruction; no claim of a complete clean-history security clearance is made.

## Pending local source

Backend has local .env.example, .gitignore, root package-lock.json and frontend gitlink changes, plus the column-only SQL migration, its execution report and RefundFlagMigrationTest.java. Keep local environment edits out of a source commit. Root package-lock.json is unrelated to Maven and was modified before this audit; preserved. Refund migration/tests and backup ignore rule are candidates for a reviewed source commit after the security/history issue is resolved. The Frontend source changes already reside in its feature-branch commits. Backend records fe as a gitlink without a tracked .gitmodules; reviewers should use the separate Frontend repository URL. No repository structure changes were attempted.

## Build/test results

- Backend: Maven offline verify using local cache PASS, 41 tests, zero failure/error/skip; JAR compiled and repackaged.
- Frontend: direct installed ESLint CLI PASS with zero warnings, node --test PASS 19 tests, Vite CLI production build PASS (73 modules). npm run itself failed because the global npm installation cannot resolve walk-up-path; no global tool installation/repair was attempted.
- Latest actual PostgreSQL startup verification passed Hibernate schema validation after the approved column migration, then failed on missing MINIO_ENDPOINT. This audit did not modify database/server configuration, restart services or substitute fake external service data.

## Outcome

No new commit hash; requested commit message was not used because the security gate failed. No push to main, no remote CI result claimed. Rotation/revocation and a repository-history handling decision require the owner's action before continuing safely.

## Follow-up: Admin image task, 13 October 2026

Both origins fetched successfully. Pending source checked against configured secrets, with no matches. Historical credential finding remains confirmed and reachable from origin/main; publication to main is still stopped under the owner's earlier instruction. New source commits are prepared locally on existing branches after 47 Backend and 23 Frontend tests/builds passed; no remote merge, history rewrite or push. Frontend local commit: 6a14d4d948a55922ccf2f45cba4177d5c06f889c. See ADMIN_IMAGE_UPLOAD_REPORT.md for the unresolved real MinIO SignatureDoesNotMatch 403 and teacher checks. Earlier sections are snapshots of the original audit, not current working-tree status.
