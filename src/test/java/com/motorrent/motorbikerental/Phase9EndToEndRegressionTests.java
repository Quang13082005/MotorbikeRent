package com.motorrent.motorbikerental;

import com.motorrent.motorbikerental.dto.request.CompleteOrderRequest;
import com.motorrent.motorbikerental.entity.*;
import com.motorrent.motorbikerental.enums.*;
import com.motorrent.motorbikerental.repository.*;
import com.motorrent.motorbikerental.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Phase 9 / Case 65: Comprehensive End-to-End Regression & Hardening Tests.
 * Ensures all existing business flows (Phases 1 - 8) remain intact and integrated
 * without cross-module regression, double counting, or security bypasses.
 */
@SpringBootTest
@Transactional
class Phase9EndToEndRegressionTests {

    @Autowired UserRepository users;
    @Autowired MotorbikeRepository bikes;
    @Autowired RentalOrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired RentalContractRepository contracts;
    @Autowired MaintenanceRecordRepository maintenance;
    @Autowired RentalOrderService rentalOrderService;
    @Autowired OperationsService operationsService;

    User admin;
    User staff;
    User customer;
    Motorbike bike;

    @BeforeEach
    void setUp() {
        admin = users.save(User.builder()
                .username("admin_p9_" + System.nanoTime())
                .email("admin_p9_" + System.nanoTime() + "@example.test")
                .password("pwd")
                .role(UserRole.ADMIN)
                .status("ACTIVE")
                .build());

        staff = users.save(User.builder()
                .username("staff_p9_" + System.nanoTime())
                .email("staff_p9_" + System.nanoTime() + "@example.test")
                .password("pwd")
                .role(UserRole.STAFF)
                .status("ACTIVE")
                .build());

        customer = users.save(User.builder()
                .username("cust_p9_" + System.nanoTime())
                .email("cust_p9_" + System.nanoTime() + "@example.test")
                .password("pwd")
                .role(UserRole.CUSTOMER)
                .status("ACTIVE")
                .build());

        bike = bikes.save(Motorbike.builder()
                .name("Honda Wave Alpha Phase 9")
                .licensePlate("43A-999.99")
                .pricePerDay(120000)
                .depositAmount(2000000)
                .status(MotorbikeStatus.AVAILABLE)
                .odoKm(1500)
                .build());
    }

    private void auth(User user) {
        var auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void testCase65_CompleteRentalLifecycle_AndCrossModuleInvariants() {
        // 1. Customer creates rental order
        RentalOrder order = orders.save(RentalOrder.builder()
                .orderCode("ORD-P9-" + System.nanoTime())
                .customer(customer)
                .motorbike(bike)
                .days(3)
                .pickupDate(LocalDateTime.now().minusDays(3))
                .returnDate(LocalDateTime.now().minusHours(2))
                .totalPrice(360000)
                .depositPaid(360000)
                .balancePaid(0)
                .status(RentalStatus.CONFIRMED)
                .build());

        // Booking payment (rental fee)
        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(360000)
                .paymentMethod("CASH")
                .status("PAID")
                .direction("IN")
                .purpose("DEPOSIT")
                .transactionType("BOOKING_PAYMENT")
                .paidAt(LocalDateTime.now().minusDays(3))
                .build());

        // 2. Handover: Bike moves to RENTED, Staff receives Asset Deposit
        bike.setStatus(MotorbikeStatus.RENTED);
        bikes.save(bike);
        order.setStatus(RentalStatus.HANDED_OVER);
        order.setStaff(staff);
        orders.save(order);

        contracts.save(RentalContract.builder()
                .rentalOrder(order)
                .contractCode("CTR-P9-" + System.nanoTime())
                .assetDepositHeld(2000000)
                .handedOverAt(LocalDateTime.now().minusDays(3))
                .build());

        // Asset deposit payment recorded (Liability, NOT revenue)
        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(2000000)
                .paymentMethod("CASH")
                .status("PAID")
                .direction("IN")
                .purpose("SECURITY")
                .transactionType("ASSET_DEPOSIT")
                .componentsJson("{\"assetDeposit\":2000000}")
                .paidAt(LocalDateTime.now().minusDays(3))
                .build());

        // 3. Completion by Staff: Refund 1,700,000 + Deduct 300,000 for scratch
        auth(staff);
        CompleteOrderRequest req = CompleteOrderRequest.builder()
                .assetDepositRefundAmount(1700000)
                .assetDepositDeductionAmount(300000)
                .depositDeductionReason("Trầy xước yếm xe")
                .depositRefundMethod("CASH")
                .build();

        rentalOrderService.completeOrder(order.getId(), req, staff.getId());

        // 4. Assert Invariants
        RentalOrder completedOrder = orders.findById(order.getId()).orElseThrow();
        assertThat(completedOrder.getStatus()).isEqualTo(RentalStatus.COMPLETED);

        Motorbike freedBike = bikes.findById(bike.getId()).orElseThrow();
        assertThat(freedBike.getStatus()).isEqualTo(MotorbikeStatus.AVAILABLE);

        List<Payment> orderPayments = payments.findAll().stream()
                .filter(p -> p.getRentalOrder().getId().equals(order.getId()))
                .toList();

        // 1. Initial rental payment (IN 360,000)
        // 2. Asset deposit receipt (IN 2,000,000)
        // 3. Asset deposit refund (OUT -1,700,000)
        // 4. Deposit deduction (OUT -300,000)
        assertThat(orderPayments).hasSize(4);

        Payment refundPmt = orderPayments.stream()
                .filter(p -> "ASSET_DEPOSIT_REFUND".equals(p.getTransactionType()))
                .findFirst().orElseThrow();
        assertThat(refundPmt.getAmount()).isEqualTo(1700000);
        assertThat(refundPmt.getDirection()).isEqualTo("OUT");

        Payment deductPmt = orderPayments.stream()
                .filter(p -> "DEPOSIT_DEDUCTION".equals(p.getTransactionType()))
                .findFirst().orElseThrow();
        assertThat(deductPmt.getAmount()).isEqualTo(300000);

        RentalContract updatedContract = contracts.findByRentalOrderId(order.getId()).orElseThrow();
        assertThat(updatedContract.getCompletedAt()).isNotNull();
        assertThat(updatedContract.getAssetDepositRefundAmount()).isEqualTo(1700000);
        assertThat(updatedContract.getAssetDepositDeductionAmount()).isEqualTo(300000);
        assertThat(updatedContract.getDepositDeductionReason()).isEqualTo("Trầy xước yếm xe");
    }

    @Test
    void testCase65_FinancialIntegrityAndNoDoubleCounting() {
        // Setup order with both rental revenue and asset deposit
        RentalOrder order = orders.save(RentalOrder.builder()
                .orderCode("ORD-FIN-" + System.nanoTime())
                .customer(customer)
                .motorbike(bike)
                .days(2)
                .pickupDate(LocalDateTime.now().minusDays(2))
                .returnDate(LocalDateTime.now())
                .totalPrice(240000)
                .depositPaid(240000)
                .status(RentalStatus.COMPLETED)
                .build());

        // Rental collected: 240,000
        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(240000)
                .paymentMethod("CASH")
                .status("PAID")
                .direction("IN")
                .purpose("DEPOSIT")
                .transactionType("BOOKING_PAYMENT")
                .paidAt(LocalDateTime.now().minusDays(2))
                .build());

        // Asset deposit in: 2,000,000
        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(2000000)
                .paymentMethod("CASH")
                .status("PAID")
                .direction("IN")
                .purpose("SECURITY")
                .transactionType("ASSET_DEPOSIT")
                .paidAt(LocalDateTime.now().minusDays(2))
                .build());

        // Asset deposit refund: 2,000,000
        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(2000000)
                .paymentMethod("CASH")
                .status("PAID")
                .direction("OUT")
                .purpose("SECURITY_REFUND")
                .transactionType("ASSET_DEPOSIT_REFUND")
                .paidAt(LocalDateTime.now())
                .build());

        // Maintenance record: 50,000
        maintenance.save(MaintenanceRecord.builder()
                .motorbike(bike)
                .type("OIL_CHANGE")
                .description("Thay nhớt định kỳ")
                .cost(50000)
                .status("COMPLETED")
                .reportedBy(staff)
                .startedAt(LocalDateTime.now().minusDays(1))
                .completedAt(LocalDateTime.now())
                .build());

        // Query reports as Admin
        auth(admin);
        var report = operationsService.reports(LocalDate.now().minusDays(5), LocalDate.now().plusDays(1));

        // 1. Rental Net Revenue should be exactly 240,000 (NOT including 2,000,000 deposit)
        long rentalNet = ((Number) report.get("rentalNetRevenue")).longValue();
        assertThat(rentalNet).isEqualTo(240000L);

        // 2. Asset deposit held should be exactly 0 (2,000,000 in - 2,000,000 out)
        long depositHeld = ((Number) report.get("totalAssetDepositHeld")).longValue();
        assertThat(depositHeld).isEqualTo(0L);

        // 3. Maintenance cost should be 50,000
        long maint = ((Number) report.get("maintenanceCost")).longValue();
        assertThat(maint).isEqualTo(50000L);

        // 4. Operating profit = 240,000 - 50,000 = 190,000
        long profit = ((Number) report.get("operatingProfit")).longValue();
        assertThat(profit).isEqualTo(190000L);
    }

    @Test
    void testCase65_CrossRoleSecurityHardening() {
        // Customer cannot access admin reports
        auth(customer);
        assertThatThrownBy(() -> operationsService.reports(null, null))
                .isInstanceOf(AccessDeniedException.class);

        // Staff cannot access admin reports
        auth(staff);
        assertThatThrownBy(() -> operationsService.reports(null, null))
                .isInstanceOf(AccessDeniedException.class);

        // Staff cannot access customer data directly from operations
        assertThatThrownBy(() -> operationsService.list("customers"))
                .isInstanceOf(AccessDeniedException.class);

        // Admin has full access
        auth(admin);
        var res = operationsService.reports(null, null);
        assertThat(res).isNotNull();
    }

    @Test
    void testCase65_IdempotencyAndDoubleCompletionProtection() {
        RentalOrder order = orders.save(RentalOrder.builder()
                .orderCode("ORD-IDEMP-" + System.nanoTime())
                .customer(customer)
                .motorbike(bike)
                .days(1)
                .pickupDate(LocalDateTime.now().minusDays(1))
                .returnDate(LocalDateTime.now())
                .totalPrice(120000)
                .depositPaid(120000)
                .status(RentalStatus.HANDED_OVER)
                .staff(staff)
                .build());

        contracts.save(RentalContract.builder()
                .rentalOrder(order)
                .contractCode("CTR-IDEMP-" + System.nanoTime())
                .assetDepositHeld(1000000)
                .build());

        // Asset deposit payment recorded so heldAssetDeposit = 1,000,000
        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(1000000)
                .paymentMethod("CASH")
                .status("PAID")
                .direction("IN")
                .purpose("SECURITY")
                .transactionType("ASSET_DEPOSIT")
                .paidAt(LocalDateTime.now().minusDays(1))
                .build());

        auth(staff);
        CompleteOrderRequest req = CompleteOrderRequest.builder()
                .assetDepositRefundAmount(1000000)
                .assetDepositDeductionAmount(0)
                .depositRefundMethod("CASH")
                .build();

        // 1st completion
        rentalOrderService.completeOrder(order.getId(), req, staff.getId());
        assertThat(orders.findById(order.getId()).orElseThrow().getStatus()).isEqualTo(RentalStatus.COMPLETED);

        // 2nd completion must fail idempotently (already completed)
        assertThatThrownBy(() -> rentalOrderService.completeOrder(order.getId(), req, staff.getId()))
                .hasMessageContaining("Trạng thái đơn không hợp lệ");
    }
}
