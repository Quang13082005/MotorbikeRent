# MotoRent — verified project context

Snapshot: 2026-09-29. Read docs/gemini-handoff/CURRENT_TASK.md for the latest checkpoint; it takes precedence over stale progress notes here. This file distinguishes implementation, target requirements and uncertainty. It is not a production-readiness certificate.

## Confirmed stack and layout

- Windows PowerShell workspace D:/Motorbike-rental. No .git directory present at handoff; do not assume a clean checkout or use git reset/clean.
- Maven Java monolith, Spring Boot 4.1.1, Spring MVC REST and web controllers, Thymeleaf templates, Spring Security/JWT, Spring Data JPA/Hibernate. pom.xml declares Java 17 source; working verification uses installed JDK 21 and .verification-pom.xml. Do not downgrade dependencies to fix local permission errors.
- HTML/CSS/vanilla JavaScript, shared Thymeleaf fragments, Tailwind CDN and custom operations/customer styles. No React/Vue/npm application build. Node is used only for syntax checks or optional tooling.
- MySQL 9.0.1 observed locally. Main schema motorent_db; isolated verification schema motorent_verify_20260926. H2 is used by most automated tests. Normal MySQL isolation REPEATABLE_READ differs from H2; VehicleChangeService mutating transactions explicitly use READ_COMMITTED plus locks after a concurrency bug was reproduced and fixed.
- PayOS Java SDK for provider payment links, signature verification, reconciliation. Maps UI uses existing Leaflet-based fragments (the owner calls it Google Maps). OCR/AI/mail configuration exists; never assume real external services are verified by local tests.
- Sources: src/main/java/com/motorrent/motorbikerental/{entity,repository,service,controller/api,controller/web,dto,mapper,security,config,enums}. UI: src/main/resources/templates and static/{js,css,images}. Tests: src/test/java/com/motorrent/motorbikerental.
- .verification-pom.xml produces target/verified/motorbike-rental-0.0.1-SNAPSHOT.jar. Start-MotoRent.ps1 runs main 8080; -Verify runs clone 8082 with real PayOS disabled and external credentials replaced. Hibernate schema mode validate, demo seeding disabled by launcher.

## Database and modules (confirmed from entities)

- User -> many RentalOrder (customer_id; optional staff_id), role CUSTOMER/STAFF/ADMIN; TokenAuthentication stores token lifecycle; OtpToken and UserDocument support auth/documents.
- Motorbike -> MotorbikeBrand, MotorbikeCategory; one-to-many MotorbikeImage, MaintenanceRecord; fields include color and assetValue for dynamic contract presentation; physical status and scheduling are separate concerns.
- RentalOrder -> current Motorbike; planned pickupDate/returnDate, money snapshots, net paid counters, addresses/coordinates, identity snapshot and statuses. Original assigned bikes are preserved by new RentalVehicleChange rows, not erased from history.
- Payment -> RentalOrder. Positive amount/receivedAmount with IN/OUT, canonical transactionType, legacy purpose/status compatibility, method/reference/provider IDs, componentsJson, idempotencyKey, sourceType/sourceId and actor/time/note audit.
- RentalVehicleChange -> RentalOrder + old/new Motorbike. Phase 2 BEFORE_DELIVERY snapshots, target hold expiry, proposal status, price/fees/security/paid values, Staff/reason, applied/cancel/refund audit.
- RentalExtension and RentalContract -> RentalOrder, Review -> completed rental/customer/bike as implemented, Notification -> User and linked rental page.
- ShopSettings stores single-store settings. TravelPlace backs destination suggestions. Active Station/Promotion entities absent; historical promotion snapshot fields still exist in RentalOrder. Do not interpret this as permission to delete old rows or backups.
- SQL migrations are versioned files with explicit Java helper execution in docs/phase1-20260928 and docs/phase2-20260929; no Flyway assumption. Do not set ddl-auto=create/update to avoid writing a migration.

## Authentication and authorization (confirmed)

config/SecurityConfig.java, security/JwtAuthenticationFilter.java, JwtUtil.java and CurrentUser.java are the authority.
JWT stateless requests use authenticated cookie/Bearer and token validity records. BCrypt password encoding. CSRF cookie XSRF-TOKEN, request header X-XSRF-TOKEN; only provider webhook route exempt, and it verifies signatures.
/api/admin/** and /admin/** require ADMIN. /api/operations/** and /staff/** allow STAFF/ADMIN. Public catalog/auth endpoints are explicitly listed; remaining routes require authentication.
Services enforce current.customer/operator, owns or canRead. Staff identity must come from current context, never payload. Form logout and API logout revoke token. PreviewSecurityConfig is NOT evidence for real authorization.

## Implemented foundation and Phases 2–4

Phase 1 completed and independently documented in docs/phase1-20260928/KET-QUA.md. AvailabilityService, Financials and FinancialSummaryService centralize time overlap, financial compatibility and balances. Existing flow remains usable.
Phase 2 code exists in VehicleChangeService/Controller, RentalVehicleChange/Repository, VehicleChangeRequest; PaymentService dispatches supplementary callbacks by sourceType. Customer rental detail displays history/payment action; Staff and Admin rental pages open vehicle-changes.js dialog; rental service blocks paid rejection without recorded refund and blocks handover of pending replacement.
Replacement proposals WAITING_FOR_PAYMENT/APPLIED/CANCELLED/EXPIRED are not extra Rental states. Immediate same/cheaper change and verified top-up change keep original rental/payment. Wrong or expired received money is REVIEW_REQUIRED. Final refund records OUT and rejection atomically. See checkpoint for migration/runtime/UI completion status.
Verified suite at package checkpoint: 70 H2/integration tests passed; six MySQL concurrency cases passed; 36 operations and 12 customer HTTP checks passed. These are NOT all 65 business acceptance cases. Browser verification and final deployment checkpoint must be read separately.

## Confirmed existing API contracts relevant to continuation

- /api/rental-orders/quote, create order endpoint, /my-orders, /{id}, /pending, /{id}/confirm, /{id}/reject, /{id}/hand-over, /{id}/complete, /{id}/cancel: inspect RentalOrderApiController for exact methods and DTOs before modifying.
- /api/payment/create/{id}, /reconcile/{id}, /payos-webhook, /order/{id}/summary: booking/provider/summary. Current paid status uses PAID with canonical SUCCESS mapping.
- Phase 2 GET /api/operations/orders/{id}/replacement-options; POST /vehicle-changes/quote and /vehicle-changes under same order prefix; GET /api/rental-orders/{id}/vehicle-changes; DELETE /api/operations/vehicle-changes/{id}; POST /api/payment/vehicle-change/{id}; POST /api/operations/orders/{id}/reject-with-refund and /adjustment-refund.
- Refund payload: amount, method, reference, reason, note. Change payload: motorbikeId, reason, optional pickupType/address/lat/lng and return equivalents. A preview is advisory; server recomputes and rechecks at commit.
- /api/operations/data/{module}, /api/admin/reports and existing settlement/extension endpoints: read their controllers; do not create duplicate endpoints for equivalent operations.
- Many APIs return DTO/map directly; auth login has result.token wrapper. Do not assume a universal JSON envelope.

## Implementation Status — 9/9 Phases Completed & Verified (2026-09-30)

- Phase 1: Environment, clone database setup, baseline establishment, MySQL concurrency suite. (HOÀN TẤT)
- Phase 2: Pick up / Delivery vehicle exchange & booking deposit refund. (HOÀN TẤT - Cases 1–13)
- Phase 3: Handover, remaining rental payment & asset deposit receipt with ID hold options. (HOÀN TẤT - Cases 14–23)
- Phase 4: Rental contract extension with expiration hold & PayOS integration. (HOÀN TẤT - Cases 24–33)
- Phase 5: During-rental negotiated vehicle change, usage segment accounting, custody vs reservation. (HOÀN TẤT - Cases 34–42)
- Phase 6: Early return request by Customer & Staff audited partial rental refund. (HOÀN TẤT - Cases 43–47)
- Phase 7: Unified completion, asset deposit refund / deduction, late fee handling & overdue detection. (HOÀN TẤT - Cases 48–58)
- Phase 8: Comprehensive Admin cash flow reports (channels, rental net revenue, asset deposit held, maintenance cost, operating profit). (HOÀN TẤT - Cases 59–64)
- Phase 9: Full 65 business cases acceptance, cross-module hardening & E2E regression testing. (HOÀN TẤT - Case 65)

All 65 Business Acceptance Cases accepted and verified. 116 automated unit/integration tests and 20 MySQL concurrency tests passing 100%. Database baseline preserved: 2 orders, 2 payments = 110,000 VND.

## Unverified assumptions and risks


- Mid-day segment rounding and deduction income classification are unresolved business decisions; see BUSINESS_RULES.md.
- Real PayOS supplementary payment end-to-end has not been exercised with bank money. Tests cover local service behavior and existing signature boundary tests; live callback networking/account setup still requires an authorized environment.
- Older destination/UI/schema cleanup progress appears in previous docs, but this turn did not re-audit every old requirement. Verify current data/source for any related task; don't trust obsolete docs/HANDOFF.md paragraphs.
- Invoice-like history, fees, security held and rental paid counters must remain consistent as later phases add new money paths. Existing separate SettlementService collection endpoints can otherwise double-record final-form receipts.
- MySQL repeatable-read snapshots, stale entity state and callbacks after cancellation need concurrency tests. H2 green results alone already missed one reproduced refund race.
- Local configuration/backups contain sensitive content. Avoid exporting them into context or sharing. No new security audit certifies the entire project.

## Read next

1. docs/gemini-handoff/CURRENT_TASK.md
2. docs/gemini-handoff/BUSINESS_RULES.md
3. docs/gemini-handoff/VERIFICATION.md
4. docs/gemini-handoff/USER_REQUIREMENTS_ORIGINAL.txt (65 cases)
5. Relevant actual source/tests; SOURCE_INDEX.md lists paths without secrets.
