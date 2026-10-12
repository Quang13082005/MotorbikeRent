package com.motorrent.motorbikerental;

import com.motorrent.motorbikerental.controller.web.CustomerPageController;
import com.motorrent.motorbikerental.entity.*;
import com.motorrent.motorbikerental.enums.*;
import com.motorrent.motorbikerental.repository.*;
import com.motorrent.motorbikerental.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConcurrentModel;
import vn.payos.PayOS;
import vn.payos.type.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class PayOSReconciliationIntegrationTests {

    @Autowired UserRepository users;
    @Autowired MotorbikeRepository bikes;
    @Autowired RentalOrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired PaymentService paymentService;
    @Autowired CustomerPageController customerPageController;
    @Autowired AvailabilityService availability;

    PayOS payOS;

    User customer, otherCustomer;
    Motorbike bike;
    LocalDateTime pickupDate;
    LocalDateTime returnDate;

    void login(User u) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u.getUsername(), "test",
                        List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))));
    }

    @BeforeEach
    void setUp() {
        payOS = mock(PayOS.class);
        ReflectionTestUtils.setField(paymentService, "payOS", payOS);

        String key = UUID.randomUUID().toString().substring(0, 8);
        customer = users.save(User.builder().username("cust-" + key).email(key + "@test.invalid").password("test").role(UserRole.CUSTOMER).build());
        otherCustomer = users.save(User.builder().username("other-" + key).email("other-" + key + "@test.invalid").password("test").role(UserRole.CUSTOMER).build());
        bike = bikes.save(Motorbike.builder().name("Test Bike " + key).pricePerDay(100000).depositAmount(1000000).status(MotorbikeStatus.AVAILABLE).build());

        pickupDate = AvailabilityService.now().plusDays(2).withNano(0);
        returnDate = pickupDate.plusDays(1); // 1 ngày = 100.000 VNĐ, cọc 50% = 50.000 VNĐ
        login(customer);
    }

    RentalOrder createOrder(String suffix, RentalStatus status, LocalDateTime expiresAt) {
        return orders.saveAndFlush(RentalOrder.builder()
                .orderCode("TEST-ORD-" + suffix)
                .customer(customer)
                .motorbike(bike)
                .pickupDate(pickupDate)
                .returnDate(returnDate)
                .days(1)
                .totalPrice(100000)
                .depositDue(50000)
                .depositPaid(0)
                .balancePaid(0)
                .status(status)
                .paymentExpiresAt(expiresAt)
                .build());
    }

    Payment createPendingPayment(RentalOrder order, Long providerCode, LocalDateTime expiresAt) {
        return payments.saveAndFlush(Payment.builder()
                .rentalOrder(order)
                .amount(50000)
                .paymentMethod("PAYOS")
                .purpose("DEPOSIT")
                .transactionType("BOOKING_PAYMENT")
                .direction("IN")
                .status("PENDING")
                .providerCode(providerCode)
                .providerLinkId("link-" + providerCode)
                .expiresAt(expiresAt)
                .sourceType("BOOKING")
                .sourceId(order.getId())
                .idempotencyKey("BOOKING:" + order.getId())
                .build());
    }

    WebhookData mockWebhookData(Long code, int amount, String linkId, String ref, String txnTime) {
        return WebhookData.builder()
                .orderCode(code)
                .amount(amount)
                .description("Dat coc don " + code)
                .accountNumber("1234567890")
                .reference(ref)
                .transactionDateTime(txnTime != null ? txnTime : AvailabilityService.now().toString())
                .currency("VND")
                .paymentLinkId(linkId)
                .code("00")
                .desc("success")
                .build();
    }

    PaymentLinkData mockPaymentLinkData(Long code, int amountPaid, String status, LocalDateTime txnTime) {
        List<Transaction> txns = new ArrayList<>();
        if (txnTime != null) {
            txns.add(Transaction.builder()
                    .reference("REF-" + code)
                    .amount(amountPaid)
                    .accountNumber("1234567890")
                    .description("Dat coc don " + code)
                    .transactionDateTime(txnTime.toString())
                    .build());
        }
        return PaymentLinkData.builder()
                .id("link-" + code)
                .orderCode(code)
                .amount(50000)
                .amountPaid(amountPaid)
                .amountRemaining(Math.max(0, 50000 - amountPaid))
                .status(status)
                .createdAt(AvailabilityService.now().toString())
                .transactions(txns)
                .build();
    }

    Webhook mockWebhook(WebhookData data) {
        return Webhook.builder()
                .code("00")
                .desc("success")
                .data(data)
                .signature("mock-signature")
                .build();
    }

    @Test
    @DisplayName("Case 1: Webhook đến trước returnUrl - cập nhật thành công, sau đó returnUrl không lặp lại")
    void case1_webhookBeforeReturnUrl() throws Exception {
        Long code = 100001L;
        LocalDateTime deadline = AvailabilityService.now().plusMinutes(15);
        RentalOrder ord = createOrder("C1", RentalStatus.PENDING_PAYMENT, deadline);
        Payment pmt = createPendingPayment(ord, code, deadline);

        // Giả lập Webhook gửi đến với timestamp trước deadline
        String txnTime = AvailabilityService.now().toString();
        WebhookData hookData = mockWebhookData(code, 50000, "link-" + code, "REF-100001", txnTime);
        Webhook webhook = mockWebhook(hookData);
        when(payOS.verifyPaymentWebhookData(any())).thenReturn(hookData);

        // 1. Webhook đến trước
        paymentService.webhook(webhook);

        RentalOrder updated = orders.findById(ord.getId()).orElseThrow();
        Payment updatedPmt = payments.findById(pmt.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(RentalStatus.PENDING);
        assertThat(updated.getDepositPaid()).isEqualTo(50000);
        assertThat(updatedPmt.getStatus()).isEqualTo("PAID");
        assertThat(updatedPmt.getReceivedAmount()).isEqualTo(50000);

        // 2. Customer quay lại returnUrl sau đó (gọi paymentResult)
        ConcurrentModel model = new ConcurrentModel();
        customerPageController.paymentResult(model, ord.getId());

        // Assert: Không cộng tiền trùng, không duplicate
        RentalOrder afterReturn = orders.findById(ord.getId()).orElseThrow();
        assertThat(afterReturn.getDepositPaid()).isEqualTo(50000);
        assertThat(afterReturn.getStatus()).isEqualTo(RentalStatus.PENDING);
    }

    @Test
    @DisplayName("Case 2: returnUrl đến trước webhook - tự động reconcile qua PayOS API, webhook đến sau idempotent")
    void case2_returnUrlBeforeWebhook() throws Exception {
        Long code = 100002L;
        LocalDateTime deadline = AvailabilityService.now().plusMinutes(15);
        RentalOrder ord = createOrder("C2", RentalStatus.PENDING_PAYMENT, deadline);
        Payment pmt = createPendingPayment(ord, code, deadline);

        // Mock PayOS getPaymentLinkInformation trả về PAID
        PaymentLinkData info = mockPaymentLinkData(code, 50000, "PAID", AvailabilityService.now());
        when(payOS.getPaymentLinkInformation(eq(code))).thenReturn(info);

        // 1. Customer quay lại trang payment-result trước khi webhook tới
        ConcurrentModel model = new ConcurrentModel();
        customerPageController.paymentResult(model, ord.getId());

        RentalOrder updated = orders.findById(ord.getId()).orElseThrow();
        Payment updatedPmt = payments.findById(pmt.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(RentalStatus.PENDING);
        assertThat(updated.getDepositPaid()).isEqualTo(50000);
        assertThat(updatedPmt.getStatus()).isEqualTo("PAID");

        // 2. Sau đó Webhook mới tới
        WebhookData hookData = mockWebhookData(code, 50000, "link-" + code, "REF-100002", AvailabilityService.now().toString());
        when(payOS.verifyPaymentWebhookData(any())).thenReturn(hookData);
        paymentService.webhook(mockWebhook(hookData));

        // Assert: Vẫn giữ nguyên, không cộng thêm tiền
        RentalOrder afterHook = orders.findById(ord.getId()).orElseThrow();
        assertThat(afterHook.getDepositPaid()).isEqualTo(50000);
    }

    @Test
    @DisplayName("Case 3 & 4: Reconcile & Refresh trang nhiều lần - bảo đảm tuyệt đối Idempotent")
    void case3_reconcileMultipleTimesIsIdempotent() throws Exception {
        Long code = 100003L;
        LocalDateTime deadline = AvailabilityService.now().plusMinutes(15);
        RentalOrder ord = createOrder("C3", RentalStatus.PENDING_PAYMENT, deadline);
        Payment pmt = createPendingPayment(ord, code, deadline);

        PaymentLinkData info = mockPaymentLinkData(code, 50000, "PAID", null);
        when(payOS.getPaymentLinkInformation(eq(code))).thenReturn(info);

        // Gọi reconcile 3 lần liên tiếp
        paymentService.reconcile(ord.getId());
        paymentService.reconcile(ord.getId());
        paymentService.reconcile(ord.getId());

        // Refresh trang payment-result 2 lần
        customerPageController.paymentResult(new ConcurrentModel(), ord.getId());
        customerPageController.paymentResult(new ConcurrentModel(), ord.getId());

        RentalOrder updated = orders.findById(ord.getId()).orElseThrow();
        assertThat(updated.getDepositPaid()).isEqualTo(50000);
        assertThat(updated.getStatus()).isEqualTo(RentalStatus.PENDING);
        assertThat(payments.findByRentalOrderIdOrderByCreatedAtDesc(ord.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Case 5: Khách thanh toán trễ SAU DEADLINE - chuyển REVIEW_REQUIRED, không tự động xác nhận đơn")
    void case5_paymentPaidAfterDeadline_requiresReview() throws Exception {
        Long code = 100005L;
        // Deadline đã trôi qua 10 phút trước
        LocalDateTime deadline = AvailabilityService.now().minusMinutes(10);
        RentalOrder ord = createOrder("C5", RentalStatus.PENDING_PAYMENT, deadline);
        Payment pmt = createPendingPayment(ord, code, deadline);

        // Thanh toán lúc now (sau deadline)
        PaymentLinkData info = mockPaymentLinkData(code, 50000, "PAID", AvailabilityService.now());
        when(payOS.getPaymentLinkInformation(eq(code))).thenReturn(info);

        paymentService.reconcile(ord.getId());

        Payment updatedPmt = payments.findById(pmt.getId()).orElseThrow();
        RentalOrder updatedOrd = orders.findById(ord.getId()).orElseThrow();

        assertThat(updatedPmt.getStatus()).isEqualTo("REVIEW_REQUIRED");
        assertThat(updatedOrd.getDepositPaid()).isEqualTo(0);
        assertThat(updatedOrd.getStatus()).isEqualTo(RentalStatus.PENDING_PAYMENT);
    }

    @Test
    @DisplayName("Case 6: Scheduler đã CANCEL đơn nhưng khách ĐÃ THANH TOÁN TRƯỚC DEADLINE và XE VẪN TRỐNG -> RESTORE THÀNH CÔNG")
    void case6_schedulerCancelledButVehicleStillFree_restoreSuccess() throws Exception {
        Long code = 100006L;
        LocalDateTime deadline = AvailabilityService.now().minusMinutes(2); // Vừa hết hạn 2 phút trước
        RentalOrder ord = createOrder("C6", RentalStatus.CANCELLED, deadline);
        ord.setRejectionReason("Hết hạn thanh toán cọc");
        orders.saveAndFlush(ord);

        Payment pmt = createPendingPayment(ord, code, deadline);

        // Khách thực tế đã trả trước deadline 5 phút (lúc deadline - 3 phút)
        LocalDateTime actualPayTime = deadline.minusMinutes(3);
        PaymentLinkData info = mockPaymentLinkData(code, 50000, "PAID", actualPayTime);
        when(payOS.getPaymentLinkInformation(eq(code))).thenReturn(info);

        paymentService.reconcile(ord.getId());

        RentalOrder restoredOrd = orders.findById(ord.getId()).orElseThrow();
        Payment restoredPmt = payments.findById(pmt.getId()).orElseThrow();

        // Khẳng định: Restore thành công!
        assertThat(restoredOrd.getStatus()).isEqualTo(RentalStatus.PENDING);
        assertThat(restoredOrd.getDepositPaid()).isEqualTo(50000);
        assertThat(restoredOrd.getRejectionReason()).isNull();
        assertThat(restoredPmt.getStatus()).isEqualTo("PAID");
    }

    @Test
    @DisplayName("Case 7: Scheduler đã CANCEL đơn, khách trả trước deadline NHƯNG XE ĐÃ BỊ NGƯỜI KHÁC ĐẶT -> KHÔNG RESTORE, TRÁNH DOUBLE BOOKING")
    void case7_schedulerCancelledAndVehicleOccupied_doNotRestore_preventDoubleBooking() throws Exception {
        Long code = 100007L;
        LocalDateTime deadline = AvailabilityService.now().minusMinutes(2);
        RentalOrder ordA = createOrder("C7-A", RentalStatus.CANCELLED, deadline);
        ordA.setRejectionReason("Hết hạn thanh toán cọc");
        orders.saveAndFlush(ordA);

        Payment pmtA = createPendingPayment(ordA, code, deadline);

        // Khách hàng khác (otherCustomer) đã đặt xe đó trong cùng khung giờ và đơn đó đã CONFIRMED
        orders.saveAndFlush(RentalOrder.builder()
                .orderCode("OCCUPIED-ORD")
                .customer(otherCustomer)
                .motorbike(bike)
                .pickupDate(pickupDate)
                .returnDate(returnDate)
                .days(1)
                .totalPrice(100000)
                .depositPaid(50000)
                .status(RentalStatus.CONFIRMED)
                .build());

        // Reconcile đơn A
        LocalDateTime actualPayTime = deadline.minusMinutes(3);
        PaymentLinkData info = mockPaymentLinkData(code, 50000, "PAID", actualPayTime);
        when(payOS.getPaymentLinkInformation(eq(code))).thenReturn(info);

        paymentService.reconcile(ordA.getId());

        RentalOrder updatedOrdA = orders.findById(ordA.getId()).orElseThrow();
        Payment updatedPmtA = payments.findById(pmtA.getId()).orElseThrow();

        // Khẳng định: TUYỆT ĐỐI KHÔNG RESTORE ĐƠN A, tránh Double Booking!
        assertThat(updatedOrdA.getStatus()).isEqualTo(RentalStatus.CANCELLED);
        assertThat(updatedOrdA.getDepositPaid()).isEqualTo(0);
        assertThat(updatedPmtA.getStatus()).isEqualTo("REVIEW_REQUIRED");
        assertThat(updatedPmtA.getNote()).contains("xe đã bị đơn khác đặt");
    }
}
