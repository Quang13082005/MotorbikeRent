# MotoRent — business requirements and implementation boundaries

Updated 2026-09-29. These are owner requirements, not a declaration that every feature exists. For implementation state read PROJECT_CONTEXT.md and CURRENT_TASK.md. Full original Vietnamese specification and all 65 cases are preserved in USER_REQUIREMENTS_ORIGINAL.txt; the approved nine-phase plan is linked in APPROVED_PLAN_REFERENCE.txt.

## Priority and superseded decisions

The owner initially wanted only two final Staff buttons and no financial fields. The later detailed specification explicitly requests actual amounts, method, reason and audit on those final actions. Preserve the simple workflow and implement those final financial forms; do not reintroduce CCCD/collection checkboxes.
An earlier decision prohibited early handover; the later approved specification permits it when physically ready and schedule-safe. Phase 3 implements this availability-checked behavior. Do not request the same approval again.
Earlier instructions approved 0 cc for electric vehicles, then explicitly removed ALL electric-vehicle support. The latter prevails. Do not reintroduce electric categories/fixtures/customer cards. Historical backups are evidence, not live catalog.
The historical pre-coding pause ended when the owner approved the plan. Work sequentially with phase reports. Do not invent unresolved policies for Phase 5 or 7.

## Earlier product requirements to preserve and verify during regression

- One store only. Remove active stations relationships/module; store settings remain useful. Do not drop payment/history tables just because PayOS is used: PayOS does not replace the application's audit ledger.
- No promotions module, admin promotion page or promotion input in contracts. Historical snapshot columns may still exist; never silently delete financial history while cleaning features.
- No electric vehicles in active frontend, backend or database.
- Vietnamese UTF-8 on profile/history and all other pages; real DB-backed identities, prices, statistics, pagination and actions. 'Hashcode' in the conversation referred to hardcoded/mock data, not password hashing. Never remove password hashing.
- Preserve original catalog search styling where restored; desktop recommendation grid: three cards per row, up to six recommended/popular vehicles per selected category (xe ga, xe số, xe côn). Do not fabricate six vehicles if DB has fewer.
- Remove the 'Tìm xe theo nhu cầu' block below travel suggestions.
- Delivery map opens on explicit location selection/click only, never each search/filter submit.
- Selected location persists from landing page to catalog to contract pickup and return until the customer explicitly changes it. Do not overwrite one independently edited leg accidentally.
- Reject past pickup times and return <= pickup on server as well as client, using Asia/Ho_Chi_Minh server time; compare full date/time.
- One identity document: asset deposit xe số 1,000,000 VND, xe ga 2,000,000 VND, xe côn 1,500,000 VND under current category/vehicle configuration. Two different CCCDs: asset deposit 0. Reject duplicate CCCD values. UI totals must update immediately; backend price is authoritative.
- travel_places scope: all NEW Đà Nẵng territory, including former Quảng Nam (Hội An included). Earlier 'Đà Nẵng only' does NOT mean old municipal boundaries. Use genuine source places/images, not arbitrary 28-row caps; verify geography and suitable image URLs before import.
- Preserve Customer, Staff and Admin functionality; audit uncovered logic changes outside the approved plan for owner review before applying them.

## Core scheduling and statuses

AVAILABLE alone never means free for a requested interval. Check overlapping live orders and active unexpired payment holds. Half-open intervals allow one rental to end exactly when another starts, subject to physical readiness. A bike physically out and overdue must not become bookable solely because the planned return passed.
Check availability in listings/quotes, selection and transactional commit; lock affected vehicles ascending by ID, then order, proposal, payment. Revalidate associations after waiting. Use tests on MySQL to catch stale snapshots/concurrency.
Main business states map to existing enum: PENDING_PAYMENT -> PENDING (paid, awaiting Staff) -> CONFIRMED -> HANDED_OVER (Đang thuê) -> COMPLETED. CANCELLED/REJECTED terminal. Legacy READY_FOR_HANDOVER, RETURN_REQUESTED, RETURNED, EXTENDED and OVERDUE exist for compatibility; do not build new mandatory steps from them. Proposal/payment states are separate from Rental workflow.
Only the authenticated operator can perform Staff financial actions; Customer reads/manages only own allowed records. Staff cannot gain Admin settings/report/customer/staff administration rights.

## Phase 1 — money foundation (implemented)

Reuse Payment as the unified cash movement, no parallel transaction ledger. A booking PayOS amount is the configured percentage of rental charge (currently 50%) plus delivery + return fees, NOT asset deposit.
Example: rent 600,000 + pickup/return 100,000 -> booking 400,000; rental balance 300,000; asset deposit separate.
Store positive integer VND amount plus IN/OUT. Successful stored status PAID (legacy REFUNDED also recognized) maps to semantic SUCCESS. Existing DEPOSIT/BALANCE/SECURITY purposes map to BOOKING_PAYMENT/RENTAL_REMAINING/ASSET_DEPOSIT. Preserve API compatibility via Financials/PaymentMapper.
One Payment can have componentsJson whose sum equals the payment amount. Components allocate money, they are not additional cash movements. Separate deposited assets from rental cash and revenue. Keep original records; new movements have actor/time/reason/method/source/idempotency/reference.
Verified server callback/reconciliation must match provider code/link/amount, valid order/proposal and schedule. Wrong amount or late/stale success is REVIEW_REQUIRED, not an automatic reservation or silently discarded money. Repeated callback must not post twice.

## Phase 2 — BEFORE_DELIVERY vehicle change (implemented)

Staff contacts the customer outside the site first. No contact checklist/status is needed.
If customer declines alternatives, Staff refunds outside the site, enters actual amount/method/reason/note/reference, then selects 'Xác nhận đã hoàn tiền'. Atomically record BOOKING_REFUND OUT with actor/time and mark REJECTED. Never mark a paid order rejected/refunded beforehand. Example IN 400,000 minus OUT 400,000 gives net cash 0.
If customer accepts: retain SAME rental ID and original booking Payment. List only vehicles available throughout planned pickup -> planned return, including image/name/category/plate/rate/total/security deposit and price difference. Recheck in the transaction.
Because no vehicle has been used, price the ENTIRE planned rental at the replacement's rate. Preserve old/new snapshots and original vehicle history.
- Equal price: retain paid amount, no additional payment needed.
- Lower price: reduce remaining due. If paid > new total, remaining = 0 and credit = paid - total. Staff explicitly records BOOKING_ADJUSTMENT_REFUND OUT for actual excess refund.
- Higher price or changed fees: required booking = configured percent of new rent + all new pickup/return fees. Top-up = max(required booking - net amount already paid toward rental, 0). Create PayOS for this amount only. Do not change vehicle, price or paid counters before valid success. If top-up = 0, change atomically on Staff confirmation.
Reprice changed delivery/return locations on the server. Keep separate legs. If legacy data lacks per-leg fee snapshots, do not guess their split.
Hold the proposed vehicle during configured payment validity. Expiry, cancel or closed parent releases the hold. Late/mismatched payment goes to review, cannot revive/alter the order.
History: BEFORE_DELIVERY, old/new bike, old/new rental and fees, totals, paid amount, top-up, credit/actual refund, reason, proposing Staff, cancellation/refund actor, timestamps.

## Phase 3 — delivery and actual collection (implemented; final verification recorded)

Staff checks documents, identity, agreement, bike and bank/cash receipt physically. One compact final form: actual rental remaining collected, asset deposit collected and method CASH/BANK_TRANSFER/OTHER, optional explicitly recorded early pickup fee if applicable. No physical-task checkboxes.
On 'Đã giao xe': validate money and readiness, record RENTAL_REMAINING IN and ASSET_DEPOSIT IN once, actualPickupTime, deliveredByStaffId and deliveredAt, switch Rental to existing HANDED_OVER and bike RENTED, all atomically.
Prefill authoritative required amounts; reject negative/insufficient required collection unless owner later introduces credit policy. Avoid charging again if older separate collection endpoints already recorded amounts.
Allow early pickup only when actual vehicle is ready and the extended occupied interval [actual pickup, planned return) is conflict-free. Preserve planned times. Do not invent an automatic early fee.

## Phase 4 — extension + PayOS (implemented; locally verified)

Customer requests a later return. Check current bike availability. Staff approval sets extension WAITING_FOR_PAYMENT; it must NOT immediately change order.returnDate.
Customer pays the full approved additional rental charge through PayOS. Valid success posts EXTENSION_PAYMENT, updates planned return/total once and marks extension paid. Pending holds expire and block conflicts until then. Late/stale success needs review.
If conflict: explain that the bike has a subsequent booking and show configured hotline/contact for possible replacement. Do not force a new rental.
ExtensionService now preserves the existing schedule until verified success. Main runtime has not been switched to the new artifact; review CURRENT_TASK.md for exact deployment state. Real-provider end-to-end remains unverified.

## Phase 5 — DURING_RENTAL replacement (not implemented yet)

Keep the used part priced at the old rate; price only the future segment at the new rate. Support multiple changes per rental with retained usage segments/history. Do not reprice earlier usage or mutate past Payments.
Find replacement availability for planned change time -> new planned return; Staff agrees real-world place/fee/reason with customer.
Example future rent 360,000 + change delivery 100,000 = PayOS 460,000, components EXTENSION_PAYMENT 360,000 and VEHICLE_CHANGE_FEE 100,000. Dates/financial approval change only after verified success.
Payment success does NOT physically release the old vehicle. Staff performs exchange, then one 'Xác nhận đã đổi xe' records actualVehicleChangeTime/operator, old returned bike availability and new physical occupancy atomically.
Higher asset deposit: collect only the difference as ASSET_DEPOSIT_ADDITIONAL IN; not revenue. Lower deposit: keep held balance until final return.
Owner clarification 2026-09-29: both BEFORE_DELIVERY and DURING_RENTAL changes must originate with Staff after contacting the Customer and agreeing the details. Customer cannot choose a replacement and mutate the rental independently. Staff enters the agreed information; Customer may view/pay the resulting proposal. This is not permission to bypass availability, backend authorization, payment verification or physical exchange confirmation.

Implementation direction: use an explicit Staff-entered agreement for DURING_RENTAL financial details rather than inventing automatic hourly rounding. Capture future rental charge, any explicitly agreed unused-rental credit, exchange fee and reason separately; retain prior usage and payment records. Quote must show arithmetic and amount due before submission, never silently refund or erase history. The owner has not specified an automatic rounding/refund formula. Actual exchange time is an audit fact and must not silently alter an already paid agreement. BEFORE_DELIVERY retains existing server pricing unless a further explicit pricing change is requested. Final field semantics and financial invariants still need implementation and verification; this paragraph does not claim the feature exists.

## Phase 6 — early return (not implemented yet)

Customer request records intent and shows hotline; it does not complete, refund or free the vehicle. Staff decides actual refund (including zero) and reason/method/note after discussing physically.
On final completion after vehicle return: actual return, completion actor/time, COMPLETED and bike AVAILABLE. If rental refund >0, post EARLY_RETURN_REFUND OUT; zero still needs decision audit. Bound refund to eligible net paid rental amount, not asset deposits. Cancel stale future proposals.

## Phase 7 — completion, asset refund/deduction, lateness (not implemented yet)

Staff inspects keys, documents, vehicle, additional charges and refunds outside the site. One final form and 'Hoàn tất đơn'; no waiting-for-inspection/refund/completion statuses.
Record actual returned time and completion Staff; reconcile asset held = actual refunded + authorized deduction. Example held 2,000,000 -> ASSET_DEPOSIT_REFUND OUT 1,700,000 + DEPOSIT_DEDUCTION 300,000 with reason. Deduction is not a second bank cash receipt.
Record actual late fee as LATE_FEE IN/method/reason if agreed. Overdue derives from effective paid schedule; unpaid extension does not erase lateness. Do not invent automatic late charge policy.
All money/audit/final status/availability in one idempotent transaction. Customer review only own completed order, once.
BLOCKED POLICY: classify deductions as cost recovery vs recognized income with owner before Phase 7/8. Never treat all held deposits or all deductions as revenue by assumption.

## Phase 8 — Admin financial reporting (not implemented yet)

Show PayOS, Staff cash/bank collection, rental, delivery/return, extension, vehicle change fee, late fee, refunds, held/refunded/deducted asset deposits and net revenue, with drill-down to Payments and actor/time.
Separate cash movement date from contract completion date. Do not aggregate both Rental.totalPrice and Payments. Separate money requiring review from recognized receipts. Asset deposits/refunds are excluded from income. Valid income/deductions require settled policy.
Old summary/report endpoints exist but are NOT proof of the target net-revenue calculation.

## Phase 9 — acceptance and handoff (not complete)

Run all 65 cases in original specification plus concurrency, cross-role access, CSRF, retries, wrong/late/duplicate callbacks, stale proposals, migrations and full UI workflow. Map each case to evidence; mark manual/offline actions and unverified real provider paths honestly. A passing suite count is not a case-by-case acceptance matrix.
