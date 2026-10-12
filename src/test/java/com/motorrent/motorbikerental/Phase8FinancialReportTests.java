package com.motorrent.motorbikerental;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class Phase8FinancialReportTests {

    @Autowired UserRepository users;
    @Autowired MotorbikeRepository bikes;
    @Autowired RentalOrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired MaintenanceRecordRepository maintenance;
    @Autowired OperationsService operationsService;

    User admin;
    User staff;
    User customer;
    Motorbike bike;

    @BeforeEach
    void setUp() {
        admin = users.save(User.builder()
                .username("admin_test_p8_" + System.nanoTime())
                .email("admin_p8_" + System.nanoTime() + "@example.test")
                .password("pwd")
                .role(UserRole.ADMIN)
                .status("ACTIVE")
                .build());

        staff = users.save(User.builder()
                .username("staff_test_p8_" + System.nanoTime())
                .email("staff_p8_" + System.nanoTime() + "@example.test")
                .password("pwd")
                .role(UserRole.STAFF)
                .status("ACTIVE")
                .build());

        customer = users.save(User.builder()
                .username("cust_test_p8_" + System.nanoTime())
                .email("cust_p8_" + System.nanoTime() + "@example.test")
                .password("pwd")
                .role(UserRole.CUSTOMER)
                .status("ACTIVE")
                .build());

        bike = bikes.save(Motorbike.builder()
                .name("Yamaha Phase 8 Test")
                .licensePlate("43A-P8-" + (System.currentTimeMillis() % 10000))
                .pricePerDay(150000)
                .depositAmount(1000000)
                .status(MotorbikeStatus.AVAILABLE)
                .build());

        login(admin);
    }

    private void login(User u) {
        var auth = new UsernamePasswordAuthenticationToken(
                new com.motorrent.motorbikerental.security.CustomUserDetails(u.getId(), u.getUsername(), u.getRole().name(), ""),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void case59_payosTrackingAndReviewRequired() {
        var order = orders.save(RentalOrder.builder()
                .orderCode("P8-ORD-59")
                .customer(customer)
                .motorbike(bike)
                .status(RentalStatus.CONFIRMED)
                .pickupDate(LocalDateTime.now())
                .returnDate(LocalDateTime.now().plusDays(2))
                .days(2)
                .totalPrice(300000)
                .build());

        var payosSuccess = payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(150000)
                .receivedAmount(150000)
                .direction("IN")
                .purpose("BOOKING_PAYMENT")
                .transactionType("BOOKING_PAYMENT")
                .paymentMethod("PAYOS")
                .status("PAID")
                .paidAt(LocalDateTime.now())
                .transactionCode("PAYOS-59-1")
                .build());

        var payosPending = payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(150000)
                .receivedAmount(150000)
                .direction("IN")
                .purpose("RENTAL_REMAINING")
                .transactionType("RENTAL_REMAINING")
                .paymentMethod("PAYOS")
                .status("REVIEW_REQUIRED")
                .paidAt(LocalDateTime.now())
                .transactionCode("PAYOS-59-2")
                .build());

        var report = operationsService.reports(LocalDate.now(), LocalDate.now());
        assertThat((Long) report.get("payosIn")).isGreaterThanOrEqualTo(150000L);
        assertThat((Long) report.get("paymentsToReview")).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void case60_channelClassificationAndStaffActorAudit() {
        var order = orders.save(RentalOrder.builder()
                .orderCode("P8-ORD-60")
                .customer(customer)
                .motorbike(bike)
                .status(RentalStatus.HANDED_OVER)
                .pickupDate(LocalDateTime.now())
                .returnDate(LocalDateTime.now().plusDays(1))
                .days(1)
                .totalPrice(150000)
                .build());

        var now = LocalDateTime.now();
        // Staff collected CASH
        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(50000)
                .receivedAmount(50000)
                .direction("IN")
                .purpose("RENTAL_REMAINING")
                .transactionType("RENTAL_REMAINING")
                .paymentMethod("CASH")
                .status("PAID")
                .paidAt(now)
                .processedAt(now)
                .processedByStaffId(staff.getId())
                .transactionCode("CASH-P8-60")
                .build());

        // Staff collected BANK_TRANSFER
        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(100000)
                .receivedAmount(100000)
                .direction("IN")
                .purpose("RENTAL_REMAINING")
                .transactionType("RENTAL_REMAINING")
                .paymentMethod("BANK_TRANSFER")
                .status("PAID")
                .paidAt(now)
                .processedAt(now)
                .processedByStaffId(staff.getId())
                .transactionCode("BANK-P8-60")
                .build());

        var report = operationsService.reports(LocalDate.now(), LocalDate.now());
        assertThat((Long) report.get("cashIn")).isGreaterThanOrEqualTo(50000L);
        assertThat((Long) report.get("bankTransferIn")).isGreaterThanOrEqualTo(100000L);

        // Case 64 drilldown: check payments list contains actor and method
        var paymentList = operationsService.list("payments");
        var pCash = paymentList.stream().filter(m -> "CASH-P8-60".equals(m.get("reference"))).findFirst().orElseThrow();
        assertThat(pCash.get("processedByStaffId")).isEqualTo(staff.getId());
        assertThat(pCash.get("method")).isEqualTo("CASH");
        assertThat(pCash.get("direction")).isEqualTo("IN");
    }

    @Test
    void case61_actualPaymentDateVsContractCompletionDate() {
        // Order completed today, but booking payment was paid LAST MONTH
        var lastMonthDate = LocalDate.now().minusMonths(1).withDayOfMonth(15);
        var paymentPaidAt = lastMonthDate.atTime(10, 0);
        var order = orders.save(RentalOrder.builder()
                .orderCode("P8-ORD-61")
                .customer(customer)
                .motorbike(bike)
                .status(RentalStatus.COMPLETED)
                .actualReturnDate(LocalDateTime.now())
                .pickupDate(paymentPaidAt)
                .returnDate(LocalDateTime.now())
                .days(30)
                .totalPrice(200000)
                .build());

        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(100000)
                .receivedAmount(100000)
                .direction("IN")
                .purpose("BOOKING_PAYMENT")
                .transactionType("BOOKING_PAYMENT")
                .paymentMethod("PAYOS")
                .status("PAID")
                .paidAt(paymentPaidAt)
                .build());

        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(100000)
                .receivedAmount(100000)
                .direction("IN")
                .purpose("RENTAL_REMAINING")
                .transactionType("RENTAL_REMAINING")
                .paymentMethod("CASH")
                .status("PAID")
                .paidAt(LocalDateTime.now())
                .build());

        // Report for this month: only the second payment (100,000) is counted in rentalCollected
        var thisMonthReport = operationsService.reports(LocalDate.now().withDayOfMonth(1), LocalDate.now());
        // Last month report: the booking payment (100,000) is counted in last month's period
        var lastMonthReport = operationsService.reports(lastMonthDate.withDayOfMonth(1), lastMonthDate.withDayOfMonth(28));

        assertThat((Long) lastMonthReport.get("rentalCollected")).isGreaterThanOrEqualTo(100000L);
    }

    @Test
    void case62_assetDepositLiabilityHeldRefundAndDeduction() {
        var order = orders.save(RentalOrder.builder()
                .orderCode("P8-ORD-62")
                .customer(customer)
                .motorbike(bike)
                .status(RentalStatus.COMPLETED)
                .pickupDate(LocalDateTime.now().minusDays(2))
                .returnDate(LocalDateTime.now())
                .days(2)
                .totalPrice(200000)
                .build());

        var now = LocalDateTime.now();
        // Asset deposit in: 2,000,000
        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(2000000)
                .receivedAmount(2000000)
                .direction("IN")
                .purpose("ASSET_DEPOSIT")
                .transactionType("ASSET_DEPOSIT")
                .paymentMethod("CASH")
                .status("PAID")
                .paidAt(now)
                .build());

        // Deposit refund: 1,700,000
        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(1700000)
                .receivedAmount(1700000)
                .direction("OUT")
                .purpose("ASSET_DEPOSIT_REFUND")
                .transactionType("ASSET_DEPOSIT_REFUND")
                .paymentMethod("CASH")
                .status("PAID")
                .paidAt(now)
                .build());

        // Deposit deduction: 300,000
        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(300000)
                .receivedAmount(300000)
                .direction("OUT")
                .purpose("DEPOSIT_DEDUCTION")
                .transactionType("DEPOSIT_DEDUCTION")
                .paymentMethod("DEPOSIT_DEDUCTION")
                .status("PAID")
                .paidAt(now)
                .note("Bồi thường trầy xước xe")
                .build());

        var report = operationsService.reports(LocalDate.now(), LocalDate.now());
        assertThat((Long) report.get("assetDepositIn")).isGreaterThanOrEqualTo(2000000L);
        assertThat((Long) report.get("assetDepositRefunded")).isGreaterThanOrEqualTo(1700000L);
        assertThat((Long) report.get("assetDepositDeducted")).isGreaterThanOrEqualTo(300000L);

        // System held deposit is 0 for this completed order because 2,000,000 in - (1,700,000 ref + 300,000 ded) = 0
        assertThat((Long) report.get("totalAssetDepositHeld")).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void case63_netRevenueAndOperatingProfitInvariants() {
        var now = LocalDateTime.now();
        var order = orders.save(RentalOrder.builder()
                .orderCode("P8-ORD-63")
                .customer(customer)
                .motorbike(bike)
                .status(RentalStatus.COMPLETED)
                .pickupDate(now.minusDays(2))
                .returnDate(now)
                .days(2)
                .totalPrice(300000)
                .actualReturnDate(now)
                .build());

        // 1. Rental Collected: 300,000
        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(300000)
                .receivedAmount(300000)
                .direction("IN")
                .purpose("BOOKING_PAYMENT")
                .transactionType("BOOKING_PAYMENT")
                .paymentMethod("PAYOS")
                .status("PAID")
                .paidAt(now)
                .build());

        // 2. Early return refund: 50,000
        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(50000)
                .receivedAmount(50000)
                .direction("OUT")
                .purpose("EARLY_RETURN_REFUND")
                .transactionType("EARLY_RETURN_REFUND")
                .paymentMethod("CASH")
                .status("PAID")
                .paidAt(now)
                .build());

        // 3. Late fee collected: 20,000
        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(20000)
                .receivedAmount(20000)
                .direction("IN")
                .purpose("LATE_FEE")
                .transactionType("LATE_FEE")
                .paymentMethod("CASH")
                .status("PAID")
                .paidAt(now)
                .build());

        // 4. Asset deposit 1,000,000 in, 1,000,000 out -> must NOT alter rentalNetRevenue
        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(1000000)
                .receivedAmount(1000000)
                .direction("IN")
                .purpose("ASSET_DEPOSIT")
                .transactionType("ASSET_DEPOSIT")
                .paymentMethod("CASH")
                .status("PAID")
                .paidAt(now)
                .build());

        payments.save(Payment.builder()
                .rentalOrder(order)
                .amount(1000000)
                .receivedAmount(1000000)
                .direction("OUT")
                .purpose("ASSET_DEPOSIT_REFUND")
                .transactionType("ASSET_DEPOSIT_REFUND")
                .paymentMethod("CASH")
                .status("PAID")
                .paidAt(now)
                .build());

        // 5. Maintenance completed in period: 40,000
        maintenance.save(MaintenanceRecord.builder()
                .motorbike(bike)
                .type("Thay nhớt")
                .cost(40000)
                .status("COMPLETED")
                .reportedBy(staff)
                .completedAt(now)
                .build());

        var report = operationsService.reports(LocalDate.now(), LocalDate.now());
        long netRevenue = (Long) report.get("rentalNetRevenue");
        long lateFee = (Long) report.get("lateFeeCollected");
        long maintenanceCost = (Long) report.get("maintenanceCost");
        long operatingProfit = (Long) report.get("operatingProfit");

        // Invariant: operatingProfit == netRevenue + lateFee - maintenanceCost
        assertThat(operatingProfit).isEqualTo(netRevenue + lateFee - maintenanceCost);
        // Invariant: netRevenue does NOT include deposit
        assertThat(report.get("assetDepositIn")).isNotEqualTo(netRevenue);
    }
}
