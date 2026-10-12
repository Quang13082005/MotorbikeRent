# Verification and local operation runbook

Run from D:/Motorbike-rental in PowerShell. Inspect CURRENT_TASK.md first. Commands below are recipes, not claims that a future run succeeded. Latest Phase 4 logs/report are in docs/phase4-20260929. Phase 3 and Phase 2 logs remain historical evidence. Keep server 8082 running under the current instruction. Always clean clone-only Phase3Fixture in finally. Payment list checks use GET /api/payment/order/{id}.

## Prerequisites and tools

An agent needs filesystem read/write in this workspace, terminal execution, installed JDK/Maven, local MySQL connectivity and browser tooling for UI tests. A plain chat without these tools cannot autonomously edit/run this project. Codex's Browser tool is not automatically available in Gemini; use Antigravity's supported browser tool and report if unavailable.
Use existing local credentials from configuration internally; do not paste them into a prompt. Direct JDBC already works here; MySQL MCP is optional, not necessary. Never enable broad MySQL root network access just for an agent.
PowerShell 7 is needed for scripts using -SkipHttpErrorCheck. Run `Get-Command pwsh,java,python,node -ErrorAction SilentlyContinue` to inspect installed tools without exposing secrets.

## Build and test

```powershell
Set-Location -LiteralPath 'D:/Motorbike-rental'
$env:JAVA_HOME='C:/Program Files/Java/jdk-21'
$motoMaven='C:/Users/THINKPAD789/.m2/wrapper/dists/apache-maven-3.9.11-bin/6mqf5t809d9geo83kj4ttckcbc/apache-maven-3.9.11/bin/mvn.cmd'
& $motoMaven -f .verification-pom.xml -o '-Dmaven.repo.local=C:/Users/THINKPAD789/.m2/repository' test
if ($LASTEXITCODE -ne 0) { throw 'Tests failed' }
```

Capture output into a new phase-specific log; do not overwrite old successful evidence when an unrelated experiment fails. Offline `-o` assumes dependencies already cached. If missing, identify the missing artifact rather than changing dependency versions. Sandbox AccessDenied for a JAR real path is a permissions problem, not a compilation bug; request required local access through the host, without disabling security.

Meaningful MySQL concurrency checks (ONLY clone):

```powershell
& $motoMaven -f .verification-pom.xml -o '-Dmaven.repo.local=C:/Users/THINKPAD789/.m2/repository' '-Dphase1.mysql=true' '-Dtest=Phase1ConcurrencyTests' test
if ($LASTEXITCODE -ne 0) { throw 'MySQL concurrency tests failed' }
```

The class name/property are historical; it now includes Phase 2 races as well. It overrides datasource to the allowlisted clone and cleans its own namespaced fixture rows. Verify baseline counts afterward. Do not run this with a main datasource override.

```powershell
node --check src/main/resources/static/js/vehicle-changes.js
node --check src/main/resources/static/js/operations-live.js
```

If frontend changes affect other scripts, check those too. Syntax checks do not prove DOM interactions.

## Packaging and running

`package` includes tests. A running JAR can prevent Maven replacing it on Windows. First identify ONLY this project's processes, then stop those exact PIDs if authorized by the current local restart task. Do not kill all Java processes.

```powershell
$motoProcesses=Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
  Where-Object { $_.CommandLine -match 'D:[/\]Motorbike-rental[/\]target[/\]verified[/\]motorbike-rental-0.0.1-SNAPSHOT.jar' }
$motoProcesses | Select-Object ProcessId
# Only after verifying the matches belong to this local application:
$motoProcesses | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
& $motoMaven -f .verification-pom.xml -o '-Dmaven.repo.local=C:/Users/THINKPAD789/.m2/repository' package
if ($LASTEXITCODE -ne 0) { throw 'Package failed; do not launch old artifact as new' }
```

Apply required schema migration before starting the corresponding database-backed app. Then run:

```powershell
./Start-MotoRent.ps1 -Verify
# After approved main migration and clone verification:
./Start-MotoRent.ps1
```

8082 = clone, real PayOS disabled. 8080 = main local app. Both bind 127.0.0.1, use schema validate and disable demo seeding. The launcher opens hidden processes and refuses an occupied port. Logs: target/runtime/{application,stdout,stderr}-8080/8082.log. Do not expose services publicly. Check READY response and actual process command/path before saying it restarted.

## Database backups / migrations / fixture

Credentials are loaded internally by Java helper. Never print properties. V002 adds rental_vehicle_changes; V001 normalized payment fields, so do not roll back code alone to pre-V001 logic.

```powershell
$motoJava='C:/Program Files/Java/jdk-21/bin/java.exe'
$motoJdbc='C:/Users/THINKPAD789/.m2/repository/com/mysql/mysql-connector-j/9.7.0/mysql-connector-j-9.7.0.jar'
& $motoJava -cp $motoJdbc docs/phase2-20260929/DatabasePhase2.java inspect motorent_verify_20260926
& $motoJava -cp $motoJdbc docs/phase2-20260929/DatabasePhase2.java inspect motorent_db
```

Existing backups: docs/phase2-20260929/before-motorent_db.sql, before-motorent_verify_20260926.sql and source-before-phase2.zip. Do NOT overwrite them. Helpers require backup before migration. For future phases create a new helper/version/snapshot, not a reused stale backup.

If CURRENT_TASK says V002 is still pending for a schema, use the reviewed migration:

```powershell
& $motoJava -cp $motoJdbc docs/phase2-20260929/DatabasePhase2.java migrate motorent_verify_20260926
# Only after verification of clone and within approved migration scope:
& $motoJava -cp $motoJdbc docs/phase2-20260929/DatabasePhase2.java migrate motorent_db
```

Do not treat CREATE TABLE IF NOT EXISTS as an upgrade tool for an already-existing table with missing columns; inspect metadata and write an explicit subsequent migration if needed.
Phase2Fixture.java is CLONE ONLY. `create` inserts one uniquely coded UI order; NOT idempotent. `inspect` reads its state; `cleanup` deletes precisely its notifications/changes/payments/order. No bank transfer is involved. Do not create again if current checkpoint says it exists. Never use cleanup as a general data purge.
Baseline at start Phase 2: each schema had two original orders and two original payments, sum amount/received 110000. Temporary fixtures must be removed before final baseline comparison.

## HTTP and browser regression

```powershell
& docs/continuation-20260928/verify-operations.ps1
. docs/customer-fixes-20260927/verify-customer.ps1
# The customer script leaves a session; close it explicitly:
$headers['X-XSRF-TOKEN']=Csrf
$null=Invoke-RestMethod "$base/api/auth/logout" -Method Post -WebSession $session -Headers $headers
```

These scripts use existing local DEMO accounts; do not replace real user credentials or publish tokens. Operations checks currently cover 36 page/API/forbidden-role checks, customer script 12 checks. Log the actual result after execution.
For browser Phase 2 checks use the dedicated clone fixture: Staff opens change modal, compare same/dearer quotes, ensure maps remain closed until requested, submit a proposal, verify original bike/price unchanged before payment, Customer sees top-up and clone payment creation returns the expected disabled message, Staff cancels/refunds and audit shows exact actor/time. Admin action must open the same form. Clean fixtures and log out afterward. Never claim bank payment success based on disabled-provider tests.

## Backups, comparison and rollback

There is no Git baseline. Compare current source files with source-before-phase2.zip (zip entries use backslashes on Windows; normalize separators). Produce a changed-files inventory. Never blindly restore the entire zip over newer work.
SQL backups contain CREATE TABLE + INSERT, intended for restoration into an empty isolated schema, not import over a running DB. Before any rollback, preserve new writes, stop only target app, restore into a separate schema, validate schema/data/financial sums, then use matching code. Do not DROP production tables or strip columns to make old code run.
Before leaving: write evidence, running URLs, last built artifact, exact DB version/migration and any fixtures in CURRENT_TASK.md.

## Phase 4 build/runtime addendum (2026-09-29)

Current Phase 4 artifact lives in target/phase4; port 8083 is its clone runtime. Port 8082 must remain untouched and serves old Phase 3. Standard Start-MotoRent.ps1 still points to target/verified. Use CURRENT_TASK.md for latest process state; inspect PID owning port, not just Start-Process parent (PATH Java can spawn a child).

```powershell
$env:JAVA_HOME='C:/Program Files/Java/jdk-21'
$motoMaven='C:/Users/THINKPAD789/.m2/wrapper/dists/apache-maven-3.9.11-bin/6mqf5t809d9geo83kj4ttckcbc/apache-maven-3.9.11/bin/mvn.cmd'
& $motoMaven -f .phase4-verification-pom.xml -o '-Dmaven.repo.local=C:/Users/THINKPAD789/.m2/repository' test
& $motoMaven -f .phase4-verification-pom.xml -o '-Dmaven.repo.local=C:/Users/THINKPAD789/.m2/repository' '-Dphase1.mysql=true' '-Dtest=Phase1ConcurrencyTests' test
# package only when the Phase 4 JAR is not held by its running process.
# If tests already passed for identical sources, -DskipTests package is packaging evidence only.
& docs/phase4-20260929/Start-Phase4.ps1
& docs/phase4-20260929/verify-customer.ps1
& docs/phase4-20260929/verify-operations.ps1
```

V004 already applied on both schemas; do not rerun blindly. DatabasePhase4.java supports inspect/backup/migrate for the two allowlisted schemas; backup refuses overwrite. Phase4Fixture.java supports create/inspect/cleanup only on clone; inspect before create, cleanup in finally. It uses existing bike 9 and test customer; never substitute a real customer. No fixture remains at final checkpoint.

Phase 4 package-final.log has 82 passing tests then repackage failure due to a child JVM lock; package-retry.log is the successful packaging-only retry. tests-mysql-final.log contains 15 passes. Preserve this distinction in future reports.
