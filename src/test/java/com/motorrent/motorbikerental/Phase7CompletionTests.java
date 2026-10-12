package com.motorrent.motorbikerental;

import com.motorrent.motorbikerental.dto.request.CompleteOrderRequest;
import com.motorrent.motorbikerental.dto.request.ReviewRequest;
import com.motorrent.motorbikerental.entity.*;
import com.motorrent.motorbikerental.enums.*;
import com.motorrent.motorbikerental.repository.*;
import com.motorrent.motorbikerental.service.*;
import com.motorrent.motorbikerental.exception.AppException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Transactional
class Phase7CompletionTests {
    @Autowired UserRepository users;
    @Autowired MotorbikeRepository bikes;
    @Autowired RentalOrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired RentalContractRepository contracts;
    @Autowired ReviewRepository reviews;
    @Autowired RentalOrderService rentalService;
    @Autowired ReviewService reviewService;
    @Autowired FinancialSummaryService financialSummaryService;

    User customer, otherCustomer, staff;
    Motorbike bike;
    RentalOrder order;
    Payment bookingPayment, remainingPayment, depositPayment;
    RentalContract contract;

    void login(User u) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u.getUsername(), "test",
                        List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))));
    }

    @BeforeEach
    void setUp() {
        String key = UUID.randomUUID().toString();
        customer = users.save(User.builder().username("p7c-" + key).email(key + "@example.invalid").password("test").role(UserRole.CUSTOMER).build());
        otherCustomer = users.save(User.builder().username("other-" + key).email("other" + key + "@example.invalid").password("test").role(UserRole.CUSTOMER).build());
        staff = users.save(User.builder().username("p7s-" + key).email("s" + key + "@example.invalid").password("test").role(UserRole.STAFF).build());

        bike = bikes.save(Motorbike.builder().name("Test Bike " + key).pricePerDay(100000).depositAmount(2000000).status(MotorbikeStatus.RENTED).build());

        var start = AvailabilityService.now().minusDays(3).withNano(0);
        var end = start.plusDays(2); // 2 ngày = 200.000 VNĐ, đã quá hạn 1 ngày

        order = orders.saveAndFlush(RentalOrder.builder()
                .orderCode("P7-" + key)
                .customer(customer)
                .motorbike(bike)
                .pickupDate(start)
                .returnDate(end)
                .days(2)
                .dailyRate(100000)
                .rentalFee(200000)
                .totalPrice(200000)
                .depositPaid(100000)
                .balancePaid(100000)
                .securityDeposit(2000000)
                .status(RentalStatus.HANDED_OVER)
                .build());

        var now = AvailabilityService.now();
        bookingPayment = payments.save(Payment.builder()
                .rentalOrder(order).amount(100000).receivedAmount(100000).direction("IN")
                .purpose("DEPOSIT").transactionType("BOOKING_PAYMENT").paymentMethod("PAYOS").status("PAID").paidAt(start).build());

        remainingPayment = payments.save(Payment.builder()
                .rentalOrder(order).amount(100000).receivedAmount(100000).direction("IN")
                .purpose("BALANCE").transactionType("RENTAL_REMAINING").paymentMethod("CASH").status("PAID").paidAt(start).build());

        depositPayment = payments.save(Payment.builder()
                .rentalOrder(order).amount(2000000).receivedAmount(2000000).direction("IN")
                .purpose("SECURITY").transactionType("ASSET_DEPOSIT").paymentMethod("CASH").status("PAID").paidAt(start).build());

        contract = contracts.save(RentalContract.builder()
                .rentalOrder(order)
                .contractCode("HD-" + order.getOrderCode())
                .handedOverAt(start)
                .handoverStaffId(staff.getId())
                .build());
    }

    @Test
    void case48_49_50_staffCompleteOrderNormallyWithFullDepositRefund() {
        login(staff);

        var req = CompleteOrderRequest.builder()
                .assetDepositRefundAmount(2000000)
                .assetDepositDeductionAmount(0)
                .depositRefundMethod("CASH")
                .build();

        var res = rentalService.completeOrder(order.getId(), req, staff.getId());

        // Case 48, 49: Rental COMPLETED
        assertThat(res.getStatus()).isEqualTo("COMPLETED");
        var savedOrder = orders.findById(order.getId()).orElseThrow();
        assertThat(savedOrder.getStatus()).isEqualTo(RentalStatus.COMPLETED);
        assertThat(savedOrder.getActualReturnDate()).isNotNull();

        // Case 50: Vehicle AVAILABLE
        var savedBike = bikes.findById(bike.getId()).orElseThrow();
        assertThat(savedBike.getStatus()).isEqualTo(MotorbikeStatus.AVAILABLE);

        // Case 52, 53: ASSET_DEPOSIT_REFUND OUT 2,000,000đ
        var orderPayments = payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId());
        var refPayment = orderPayments.stream()
                .filter(p -> "ASSET_DEPOSIT_REFUND".equals(Financials.type(p)))
                .findFirst().orElseThrow();
        assertThat(refPayment.getDirection()).isEqualTo("OUT");
        assertThat(refPayment.getAmount()).isEqualTo(2000000);
        assertThat(refPayment.getStatus()).isEqualTo("PAID");

        // Contract audit
        var savedContract = contracts.findByRentalOrderId(order.getId()).orElseThrow();
        assertThat(savedContract.getCompletedAt()).isNotNull();
        assertThat(savedContract.getCompletionStaffId()).isEqualTo(staff.getId());
        assertThat(savedContract.getAssetDepositHeld()).isEqualTo(2000000);
        assertThat(savedContract.getAssetDepositRefundAmount()).isEqualTo(2000000);
        assertThat(savedContract.getAssetDepositDeductionAmount()).isEqualTo(0);
    }

    @Test
    void case51_customerReviewMotorbikeAfterCompleted() {
        login(staff);
        rentalService.completeOrder(order.getId(), null, staff.getId());

        // Case 51: Customer can review completed order once
        login(customer);
        var reviewReq = ReviewRequest.builder()
                .orderId(order.getId())
                .rating(5.0)
                .comment("Xe chạy rất tốt, tiết kiệm xăng.")
                .build();
        reviewService.createReview(reviewReq, customer.getId());

        var bikeAfter = bikes.findById(bike.getId()).orElseThrow();
        assertThat(bikeAfter.getReviewCount()).isEqualTo(1);
        assertThat(bikeAfter.getRating()).isEqualTo(5.0);

        // Cannot review twice
        assertThatThrownBy(() -> reviewService.createReview(reviewReq, customer.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Đơn thuê đã được đánh giá");

        // Other customer cannot review this order
        login(otherCustomer);
        assertThatThrownBy(() -> reviewService.createReview(reviewReq, otherCustomer.getId()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void case52_53_54_55_depositRefundAndDeductionWithAuditing() {
        login(staff);

        var req = CompleteOrderRequest.builder()
                .assetDepositRefundAmount(1700000)
                .assetDepositDeductionAmount(300000)
                .depositDeductionReason("Bồi thường trầy xước yếm xe bên phải")
                .depositRefundMethod("BANK_TRANSFER")
                .build();

        rentalService.completeOrder(order.getId(), req, staff.getId());

        var orderPayments = payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId());
        var refPayment = orderPayments.stream()
                .filter(p -> "ASSET_DEPOSIT_REFUND".equals(Financials.type(p)))
                .findFirst().orElseThrow();
        assertThat(refPayment.getDirection()).isEqualTo("OUT");
        assertThat(refPayment.getAmount()).isEqualTo(1700000);
        assertThat(refPayment.getPaymentMethod()).isEqualTo("BANK_TRANSFER");

        var dedPayment = orderPayments.stream()
                .filter(p -> "DEPOSIT_DEDUCTION".equals(Financials.type(p)))
                .findFirst().orElseThrow();
        assertThat(dedPayment.getDirection()).isEqualTo("OUT");
        assertThat(dedPayment.getAmount()).isEqualTo(300000);
        assertThat(dedPayment.getNote()).isEqualTo("Bồi thường trầy xước yếm xe bên phải");

        // Case 55: Financial Summary check
        // assetDepositHeld should be 0 because 2,000,000 held = 1,700,000 refunded + 300,000 deducted
        var summary = financialSummaryService.forOrder(order.getId());
        assertThat(summary.get("assetDepositHeld")).isEqualTo(0L);

        // rentalCashNet should still be 200,000 (100k booking + 100k balance) - deposit deduction is NOT new cash in
        assertThat(summary.get("rentalCashNet")).isEqualTo(200000L);
    }

    @Test
    void case54_validation_deductionRequiresReasonAndCannotMismatchedHeld() {
        login(staff);

        // Missing deduction reason when deduction > 0
        var reqNoReason = CompleteOrderRequest.builder()
                .assetDepositRefundAmount(1700000)
                .assetDepositDeductionAmount(300000)
                .depositDeductionReason("   ")
                .build();
        assertThatThrownBy(() -> rentalService.completeOrder(order.getId(), reqNoReason, staff.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Phải nhập lý do khấu trừ cọc");

        // Sum does not equal held deposit (1,500,000 + 300,000 = 1,800,000 != 2,000,000)
        var reqMismatch = CompleteOrderRequest.builder()
                .assetDepositRefundAmount(1500000)
                .assetDepositDeductionAmount(300000)
                .depositDeductionReason("Lý do hợp lệ")
                .build();
        assertThatThrownBy(() -> rentalService.completeOrder(order.getId(), reqMismatch, staff.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phải bằng tổng tiền cọc đang giữ");

        // Negative values
        var reqNegative = CompleteOrderRequest.builder()
                .assetDepositRefundAmount(-100000)
                .assetDepositDeductionAmount(2100000)
                .depositDeductionReason("Lý do")
                .build();
        assertThatThrownBy(() -> rentalService.completeOrder(order.getId(), reqNegative, staff.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không được âm");
    }

    @Test
    void case56_57_58_lateFeeRecordedAsCashIn() {
        login(staff);

        // Customer returns 1 day late -> staff charges 150,000 late fee
        var req = CompleteOrderRequest.builder()
                .assetDepositRefundAmount(2000000)
                .assetDepositDeductionAmount(0)
                .lateFee(150000)
                .lateFeeReason("Khách trả xe trễ 1 ngày")
                .lateFeePaymentMethod("CASH")
                .build();

        rentalService.completeOrder(order.getId(), req, staff.getId());

        // Case 58: Transaction IN for LATE_FEE
        var orderPayments = payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId());
        var latePayment = orderPayments.stream()
                .filter(p -> "LATE_FEE".equals(Financials.type(p)))
                .findFirst().orElseThrow();
        assertThat(latePayment.getDirection()).isEqualTo("IN");
        assertThat(latePayment.getAmount()).isEqualTo(150000);
        assertThat(latePayment.getPaymentMethod()).isEqualTo("CASH");
        assertThat(latePayment.getStatus()).isEqualTo("PAID");
        assertThat(latePayment.getNote()).isEqualTo("Khách trả xe trễ 1 ngày");

        // Financial Summary: lateFee is income -> rentalCashNet increases from 200,000 to 350,000
        var summary = financialSummaryService.forOrder(order.getId());
        assertThat(summary.get("rentalCashNet")).isEqualTo(350000L);
    }

    @Test
    void idempotentCompletion_repeatedCallsDoNotDuplicatePayments() {
        login(staff);

        var req = CompleteOrderRequest.builder()
                .assetDepositRefundAmount(1500000)
                .assetDepositDeductionAmount(500000)
                .depositDeductionReason("Thiếu gương chiếu hậu")
                .lateFee(100000)
                .lateFeeReason("Trả xe trễ 2 giờ")
                .build();

        // First call
        rentalService.completeOrder(order.getId(), req, staff.getId());
        int count1 = payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId()).size();

        // Second call with same parameters should reject with AppException and NOT duplicate payments
        assertThatThrownBy(() -> rentalService.completeOrder(order.getId(), req, staff.getId()))
                .isInstanceOf(AppException.class);
        int count2 = payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId()).size();

        assertThat(count2).isEqualTo(count1);
    }
}
