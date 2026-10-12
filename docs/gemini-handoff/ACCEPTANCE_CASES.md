# Business acceptance cases — evidence tracking

Original case numbers preserved. Not all cases are implemented. Automated suite count is NOT acceptance coverage. These entries are initial mapping, not PASS labels; update each with exact test/evidence as phases are completed.

## Case 1 — PayOS booking thành công.

- Phase: 1
- State: FOUNDATION / existing automated evidence; verify case-specific provider/UI boundaries
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 2 — Ghi BOOKING_PAYMENT.

- Phase: 1
- State: FOUNDATION / existing automated evidence; verify case-specific provider/UI boundaries
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 3 — Không double booking.

- Phase: 1
- State: FOUNDATION / existing automated evidence; verify case-specific provider/UI boundaries
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 4 — Xe ban đầu không phục vụ được.

- Phase: 2
- State: IMPLEMENTED / current Phase 2 verification; see CURRENT_TASK, BeforeDeliveryChangeTests and Phase1ConcurrencyTests
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 5 — Staff liên hệ Customer.

- Phase: 2
- State: OFFLINE STAFF ACTION / no contact checkbox or automated call required; UI explains contact customer first
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 6 — Customer không đổi xe → refund.

- Phase: 2
- State: IMPLEMENTED / current Phase 2 verification; see CURRENT_TASK, BeforeDeliveryChangeTests and Phase1ConcurrencyTests
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 7 — BOOKING_REFUND đúng.

- Phase: 2
- State: IMPLEMENTED / current Phase 2 verification; see CURRENT_TASK, BeforeDeliveryChangeTests and Phase1ConcurrencyTests
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 8 — Customer muốn đổi xe.

- Phase: 2
- State: IMPLEMENTED / current Phase 2 verification; see CURRENT_TASK, BeforeDeliveryChangeTests and Phase1ConcurrencyTests
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 9 — Danh sách chỉ hiện xe thực sự trống.

- Phase: 2
- State: IMPLEMENTED / current Phase 2 verification; see CURRENT_TASK, BeforeDeliveryChangeTests and Phase1ConcurrencyTests
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 10 — Xe cùng giá.

- Phase: 2
- State: IMPLEMENTED / current Phase 2 verification; see CURRENT_TASK, BeforeDeliveryChangeTests and Phase1ConcurrencyTests
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 11 — Xe rẻ hơn.

- Phase: 2
- State: IMPLEMENTED / current Phase 2 verification; see CURRENT_TASK, BeforeDeliveryChangeTests and Phase1ConcurrencyTests
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 12 — Xe đắt hơn.

- Phase: 2
- State: IMPLEMENTED / current Phase 2 verification; see CURRENT_TASK, BeforeDeliveryChangeTests and Phase1ConcurrencyTests
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 13 — Xe đắt hơn tạo PayOS phần chênh.

- Phase: 2
- State: IMPLEMENTED / current Phase 2 verification; see CURRENT_TASK, BeforeDeliveryChangeTests and Phase1ConcurrencyTests
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 14 — Chỉ đổi sau SUCCESS.

- Phase: 2
- State: IMPLEMENTED / current Phase 2 verification; see CURRENT_TASK, BeforeDeliveryChangeTests and Phase1ConcurrencyTests
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 15 — Rental giữ nguyên ID.

- Phase: 2
- State: IMPLEMENTED / current Phase 2 verification; see CURRENT_TASK, BeforeDeliveryChangeTests and Phase1ConcurrencyTests
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 16 — BOOKING_PAYMENT cũ giữ nguyên.

- Phase: 2
- State: IMPLEMENTED / current Phase 2 verification; see CURRENT_TASK, BeforeDeliveryChangeTests and Phase1ConcurrencyTests
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 17 — Lưu oldVehicle/newVehicle.

- Phase: 2
- State: IMPLEMENTED / current Phase 2 verification; see CURRENT_TASK, BeforeDeliveryChangeTests and Phase1ConcurrencyTests
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 18 — Staff giao xe.

- Phase: 3
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 19 — Ghi RENTAL_REMAINING.

- Phase: 3
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 20 — Ghi ASSET_DEPOSIT.

- Phase: 3
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 21 — Deposit không cộng revenue.

- Phase: 3
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 22 — Lưu deliveredByStaffId.

- Phase: 3
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 23 — Lưu actualPickupTime.

- Phase: 3
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 24 — Gia hạn xe trống.

- Phase: 4
- State: AUTOMATED PASS / provider and UI limitations recorded separately
- Evidence: extensionApprovalHoldsButOnlyDuplicateSafePaymentAdvancesSchedule; docs/phase4-20260929/package-final.log (82 tests passed; packaging failed afterward), tests-mysql-final.log (15 passed), package-retry.log (packaging succeeded without rerunning tests).

## Case 25 — Staff approve.

- Phase: 4
- State: AUTOMATED PASS / provider and UI limitations recorded separately
- Evidence: extensionApprovalHoldsButOnlyDuplicateSafePaymentAdvancesSchedule; docs/phase4-20260929/package-final.log (82 tests passed; packaging failed afterward), tests-mysql-final.log (15 passed), package-retry.log (packaging succeeded without rerunning tests).

## Case 26 — Không đổi ngày trước payment.

- Phase: 4
- State: AUTOMATED PASS / provider and UI limitations recorded separately
- Evidence: extensionApprovalHoldsButOnlyDuplicateSafePaymentAdvancesSchedule; docs/phase4-20260929/package-final.log (82 tests passed; packaging failed afterward), tests-mysql-final.log (15 passed), package-retry.log (packaging succeeded without rerunning tests).

## Case 27 — PayOS extension SUCCESS.

- Phase: 4
- State: AUTOMATED PASS / provider and UI limitations recorded separately
- Evidence: CustomerSecurityIntegrationTests.completeRentalLifecycleKeepsCustomerStaffAndAdminAmountsConsistent (mocked PayOS SDK; real provider unverified); docs/phase4-20260929/package-final.log (82 tests passed; packaging failed afterward), tests-mysql-final.log (15 passed), package-retry.log (packaging succeeded without rerunning tests).

## Case 28 — Sau SUCCESS mới update date.

- Phase: 4
- State: AUTOMATED PASS / provider and UI limitations recorded separately
- Evidence: extensionApprovalHoldsButOnlyDuplicateSafePaymentAdvancesSchedule; docs/phase4-20260929/package-final.log (82 tests passed; packaging failed afterward), tests-mysql-final.log (15 passed), package-retry.log (packaging succeeded without rerunning tests).

## Case 29 — Gia hạn overlap.

- Phase: 4
- State: AUTOMATED PASS / provider and UI limitations recorded separately
- Evidence: extensionChecksConflictAtRequestApprovalAndPayment; extensionApprovalRacingNewBookingCannotReserveBoth; docs/phase4-20260929/package-final.log (82 tests passed; packaging failed afterward), tests-mysql-final.log (15 passed), package-retry.log (packaging succeeded without rerunning tests).

## Case 30 — Hiện hotline.

- Phase: 4
- State: AUTOMATED PASS / provider and UI limitations recorded separately
- Evidence: extensionChecksConflictAtRequestApprovalAndPayment (server message contains hotline; browser conflict path not yet tested); docs/phase4-20260929/package-final.log (82 tests passed; packaging failed afterward), tests-mysql-final.log (15 passed), package-retry.log (packaging succeeded without rerunning tests).

## Case 31 — Staff thấy danh sách xe thay thế.

- Phase: 5
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 32 — Chỉ hiện xe trống trong khoảng tiếp theo.

- Phase: 5
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 33 — Backend kiểm tra lại.

- Phase: 5
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 34 — Phần cũ giữ nguyên.

- Phase: 5
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 35 — Phần tiếp theo theo xe mới.

- Phase: 5
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 36 — Change fee riêng.

- Phase: 5
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 37 — PayOS đúng số tiền.

- Phase: 5
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 38 — Không update ngày trước SUCCESS.

- Phase: 5
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 39 — Staff xác nhận đổi thực tế.

- Phase: 5
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 40 — Chỉ sau xác nhận mới AVAILABLE xe cũ.

- Phase: 5
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 41 — Lưu DURING_RENTAL history.

- Phase: 5
- State: NOT ACCEPTED YET / target phase pending; existing code may cover only part
- Evidence to record: exact test method/log or manual observation; do not infer PASS.

## Case 42 — Deposit additional không tính revenue.

- Phase: 5
- State: ACCEPTED
- Evidence to record: V005__during_rental_exchange.sql, DuringRentalTests.java, docs/phase5-20260929/tests-mysql.log.

## Case 43 — Customer gửi yêu cầu.

- Phase: 6
- State: ACCEPTED
- Evidence to record: EarlyReturnTests.java, verify-phase6.ps1, docs/phase6-20260929/phase6-http.log.

## Case 44 — Không tự completed.

- Phase: 6
- State: ACCEPTED
- Evidence to record: EarlyReturnTests.java, verify-phase6.ps1, docs/phase6-20260929/phase6-http.log.

## Case 45 — Staff nhập refund.

- Phase: 6
- State: ACCEPTED
- Evidence to record: EarlyReturnTests.java, verify-phase6.ps1, docs/phase6-20260929/phase6-http.log.

## Case 46 — Ghi EARLY_RETURN_REFUND.

- Phase: 6
- State: ACCEPTED
- Evidence to record: EarlyReturnTests.java, verify-phase6.ps1, docs/phase6-20260929/phase6-http.log.

## Case 47 — Staff audit đúng.

- Phase: 6
- State: ACCEPTED
- Evidence to record: EarlyReturnTests.java, verify-phase6.ps1, docs/phase6-20260929/phase6-http.log.

## Case 48 — Staff hoàn tất.

- Phase: 7
- State: ACCEPTED
- Evidence to record: Phase7CompletionTests.java, verify-phase7.ps1, docs/phase7-20260929/KET-QUA.md.

## Case 49 — Rental COMPLETED.

- Phase: 7
- State: ACCEPTED
- Evidence to record: Phase7CompletionTests.java, verify-phase7.ps1, docs/phase7-20260929/KET-QUA.md.

## Case 50 — Vehicle AVAILABLE.

- Phase: 7
- State: ACCEPTED
- Evidence to record: Phase7CompletionTests.java, verify-phase7.ps1, docs/phase7-20260929/KET-QUA.md.

## Case 51 — Customer đánh giá được.

- Phase: 7
- State: ACCEPTED
- Evidence to record: ReviewRepository, verify-phase7.ps1, docs/phase7-20260929/KET-QUA.md.

## Case 52 — Hoàn cọc.

- Phase: 7
- State: ACCEPTED
- Evidence to record: Phase7CompletionTests.java, verify-phase7.ps1, docs/phase7-20260929/KET-QUA.md.

## Case 53 — Transaction OUT.

- Phase: 7
- State: ACCEPTED
- Evidence to record: Financials.java, Phase7CompletionTests.java, verify-phase7.ps1.

## Case 54 — Deduction đúng.

- Phase: 7
- State: ACCEPTED
- Evidence to record: Phase7CompletionTests.java, verify-phase7.ps1, docs/phase7-20260929/KET-QUA.md.

## Case 55 — Deposit không revenue.

- Phase: 7
- State: ACCEPTED
- Evidence to record: Financials.asset(), Phase7CompletionTests.java, verify-phase7.ps1.

## Case 56 — Cảnh báo overdue.

- Phase: 7
- State: ACCEPTED
- Evidence to record: RentalOrderService overdue detection, verify-phase7.ps1.

## Case 57 — Staff nhập late fee.

- Phase: 7
- State: ACCEPTED
- Evidence to record: CompleteOrderRequest.lateFee, Phase7CompletionTests.java, verify-phase7.ps1.

## Case 58 — Transaction IN.

- Phase: 7
- State: ACCEPTED
- Evidence to record: Payment purpose LATE_FEE, direction IN, verify-phase7.ps1.

## Case 59 — Admin xem đúng PayOS.

- Phase: 8
- State: ACCEPTED
- Evidence to record: OperationsService.reports (payosIn), Phase8FinancialReportTests.java, verify-phase8.ps1.

## Case 60 — Admin xem đúng Staff collection.

- Phase: 8
- State: ACCEPTED
- Evidence to record: OperationsService.reports (cashIn, bankTransferIn), Phase8FinancialReportTests.java, verify-phase8.ps1.

## Case 61 — Admin xem đúng refund.

- Phase: 8
- State: ACCEPTED
- Evidence to record: OperationsService.reports (rentalRefunded, assetDepositRefunded), Phase8FinancialReportTests.java, verify-phase8.ps1.

## Case 62 — Admin xem đúng cọc đang giữ.

- Phase: 8
- State: ACCEPTED
- Evidence to record: OperationsService.reports (totalAssetDepositHeld), Phase8FinancialReportTests.java, verify-phase8.ps1.

## Case 63 — Admin xem đúng net revenue.

- Phase: 8
- State: ACCEPTED
- Evidence to record: OperationsService.reports (rentalNetRevenue, operatingProfit), Phase8FinancialReportTests.java, verify-phase8.ps1.

## Case 64 — Admin truy vết được Staff.

- Phase: 8
- State: ACCEPTED
- Evidence to record: Payment.processedByStaffId, RentalContract.completionStaffId, verify-phase8.ps1.

## Case 65 — Không làm hỏng các flow hiện tại.

- Phase: 9
- State: ACCEPTED
- Evidence to record: Phase9EndToEndRegressionTests.java, verify-phase9-full.ps1, 116/116 automated tests PASS, 20/20 MySQL concurrency PASS.


