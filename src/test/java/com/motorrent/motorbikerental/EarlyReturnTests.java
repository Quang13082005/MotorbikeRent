package com.motorrent.motorbikerental;

import com.motorrent.motorbikerental.dto.request.CompleteOrderRequest;
import com.motorrent.motorbikerental.dto.request.EarlyReturnRequest;
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
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Transactional
class EarlyReturnTests {
    @Autowired UserRepository users;
    @Autowired MotorbikeRepository bikes;
    @Autowired RentalOrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired RentalContractRepository contracts;
    @Autowired RentalVehicleChangeRepository vehicleChanges;
    @Autowired RentalOrderService rentalService;

    User customer, otherCustomer, staff;
    Motorbike bike;
    RentalOrder order;
    Payment bookingPayment, remainingPayment;
    RentalContract contract;

    void login(User u) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(u.getUsername(), "test",
                        List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name()))));
    }

    @BeforeEach
    void setUp() {
        String key = UUID.randomUUID().toString();
        customer = users.save(User.builder().username("erc-" + key).email(key + "@example.invalid").password("test").role(UserRole.CUSTOMER).build());
        otherCustomer = users.save(User.builder().username("other-" + key).email("other" + key + "@example.invalid").password("test").role(UserRole.CUSTOMER).build());
        staff = users.save(User.builder().username("ers-" + key).email("s" + key + "@example.invalid").password("test").role(UserRole.STAFF).build());

        bike = bikes.save(Motorbike.builder().name("Test Bike " + key).pricePerDay(100000).depositAmount(1000000).status(MotorbikeStatus.RENTED).build());

        var start = AvailabilityService.now().minusDays(1).withNano(0);
        var end = start.plusDays(3); // 3 ngày = 300.000 VNĐ

        order = orders.saveAndFlush(RentalOrder.builder()
                .orderCode("ER-" + key)
                .customer(customer)
                .motorbike(bike)
                .pickupDate(start)
                .returnDate(end)
                .days(3)
                .dailyRate(100000)
                .rentalFee(300000)
                .deliveryFee(0)
                .totalPrice(300000)
                .depositDue(150000)
                .depositPaid(150000)
                .balancePaid(150000)
                .securityDeposit(1000000)
                .pickupType("STORE")
                .returnType("STORE")
                .status(RentalStatus.HANDED_OVER)
                .build());

        contract = contracts.saveAndFlush(RentalContract.builder()
                .rentalOrder(order)
                .contractCode("HD-" + order.getOrderCode())
                .handedOverAt(start)
                .handoverStaffId(staff.getId())
                .build());

        bookingPayment = payments.saveAndFlush(Payment.builder()
                .rentalOrder(order)
                .amount(150000)
                .receivedAmount(150000)
                .direction("IN")
                .purpose("DEPOSIT")
                .transactionType("BOOKING_PAYMENT")
                .paymentMethod("PAYOS")
                .status("PAID")
                .transactionCode("PAYOS-" + key)
                .build());

        remainingPayment = payments.saveAndFlush(Payment.builder()
                .rentalOrder(order)
                .amount(150000)
                .receivedAmount(150000)
                .direction("IN")
                .purpose("BALANCE")
                .transactionType("RENTAL_REMAINING")
                .paymentMethod("CASH")
                .status("PAID")
                .transactionCode("CASH-" + key)
                .build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void case43_customerCanRequestEarlyReturn() {
        login(customer);
        var res = rentalService.requestEarlyReturn(order.getId(),
                EarlyReturnRequest.builder().reason("Công việc kết thúc sớm").build(),
                customer.getId());

        assertThat(res.getStatus()).isEqualTo("RETURN_REQUESTED");

        var updatedOrder = orders.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(RentalStatus.RETURN_REQUESTED);

        var updatedContract = contracts.findByRentalOrderId(order.getId()).orElseThrow();
        assertThat(updatedContract.getEarlyReturnRequestedAt()).isNotNull();
        assertThat(updatedContract.getEarlyReturnReason()).isEqualTo("Công việc kết thúc sớm");
    }

    @Test
    void case44_earlyReturnRequestDoesNotCompleteOrderNorFreeVehicleNorRefund() {
        login(customer);
        rentalService.requestEarlyReturn(order.getId(),
                EarlyReturnRequest.builder().reason("Về quê gấp").build(),
                customer.getId());

        var updatedOrder = orders.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(RentalStatus.RETURN_REQUESTED);
        assertThat(updatedOrder.getStatus()).isNotEqualTo(RentalStatus.COMPLETED);

        // Xe vẫn RENTED, không AVAILABLE
        var currentBike = bikes.findById(bike.getId()).orElseThrow();
        assertThat(currentBike.getStatus()).isEqualTo(MotorbikeStatus.RENTED);

        // Không có bản ghi thanh toán hoàn tiền nào tự sinh ra
        var orderPayments = payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId());
        assertThat(orderPayments).hasSize(2);
        assertThat(orderPayments).noneMatch(p -> "OUT".equals(p.getDirection()));
    }

    @Test
    void case45_staffDecidesRefundAmountWithinNetRentalPaid() {
        login(customer);
        rentalService.requestEarlyReturn(order.getId(),
                EarlyReturnRequest.builder().reason("Trả sớm 1 ngày").build(),
                customer.getId());

        login(staff);
        // Khách đã trả 300.000 VNĐ tiền thuê
        // Staff nhập hoàn quá 300.000đ -> phải bị chặn
        assertThatThrownBy(() -> rentalService.completeOrder(order.getId(),
                CompleteOrderRequest.builder().refundAmount(350000).build(),
                staff.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không được vượt quá số tiền thuê khách đã thanh toán");

        // Staff nhập số âm -> phải bị chặn
        assertThatThrownBy(() -> rentalService.completeOrder(order.getId(),
                CompleteOrderRequest.builder().refundAmount(-10000).build(),
                staff.getId()))
                .isInstanceOf(IllegalArgumentException.class);

        // Staff nhập 0 đồng -> hợp lệ
        var resZero = rentalService.completeOrder(order.getId(),
                CompleteOrderRequest.builder().refundAmount(0).refundReason("Không hoàn tiền theo chính sách").build(),
                staff.getId());
        assertThat(resZero.getStatus()).isEqualTo("COMPLETED");

        var updatedContract = contracts.findByRentalOrderId(order.getId()).orElseThrow();
        assertThat(updatedContract.getEarlyReturnRefundAmount()).isZero();
        assertThat(updatedContract.getEarlyReturnDecisionReason()).isEqualTo("Không hoàn tiền theo chính sách");
        assertThat(updatedContract.getCompletionStaffId()).isEqualTo(staff.getId());
    }

    @Test
    void case46_recordsEarlyReturnRefundPaymentCorrectlyAndIdempotent() {
        login(customer);
        rentalService.requestEarlyReturn(order.getId(),
                EarlyReturnRequest.builder().reason("Trả sớm 1 ngày").build(),
                customer.getId());

        login(staff);
        // Hoàn 100.000 VNĐ
        var res = rentalService.completeOrder(order.getId(),
                CompleteOrderRequest.builder()
                        .refundAmount(100000)
                        .refundReason("Khách trả sớm 1 ngày")
                        .paymentMethod("BANK_TRANSFER")
                        .note("Đã chuyển khoản Vietcombank")
                        .build(),
                staff.getId());

        assertThat(res.getStatus()).isEqualTo("COMPLETED");

        var orderPayments = payments.findByRentalOrderIdOrderByCreatedAtDesc(order.getId());
        assertThat(orderPayments).hasSize(3);

        var refundPayment = orderPayments.stream()
                .filter(p -> "EARLY_RETURN_REFUND".equals(p.getTransactionType()))
                .findFirst().orElseThrow();

        assertThat(refundPayment.getAmount()).isEqualTo(100000);
        assertThat(refundPayment.getDirection()).isEqualTo("OUT");
        assertThat(refundPayment.getStatus()).isEqualTo("PAID");
        assertThat(refundPayment.getPaymentMethod()).isEqualTo("BANK_TRANSFER");
        assertThat(refundPayment.getProcessedByStaffId()).isEqualTo(staff.getId());
        assertThat(refundPayment.getIdempotencyKey()).isEqualTo("EARLY_RETURN_REFUND:" + order.getId());
        assertThat(Financials.cash(refundPayment)).isEqualTo(-100000);
    }

    @Test
    void case47_staffAuditAndVehicleAvailableAndProposalCancellation() {
        // Tạo một proposal đổi xe còn đang chờ thanh toán
        var change = new RentalVehicleChange();
        change.setRentalOrder(order);
        change.setOldVehicle(bike);
        change.setNewVehicle(bike);
        change.setType("DURING_RENTAL");
        change.setStatus("WAITING_FOR_PAYMENT");
        vehicleChanges.saveAndFlush(change);

        login(customer);
        rentalService.requestEarlyReturn(order.getId(),
                EarlyReturnRequest.builder().reason("Trả sớm").build(),
                customer.getId());

        login(staff);
        var res = rentalService.completeOrder(order.getId(),
                CompleteOrderRequest.builder().refundAmount(50000).refundReason("Hỗ trợ nửa ngày").build(),
                staff.getId());

        assertThat(res.getStatus()).isEqualTo("COMPLETED");

        // Xe phải trở thành AVAILABLE
        var updatedBike = bikes.findById(bike.getId()).orElseThrow();
        assertThat(updatedBike.getStatus()).isEqualTo(MotorbikeStatus.AVAILABLE);

        // Đơn phải có actualReturnDate
        var updatedOrder = orders.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getActualReturnDate()).isNotNull();

        // Contract phải audit đủ
        var updatedContract = contracts.findByRentalOrderId(order.getId()).orElseThrow();
        assertThat(updatedContract.getCompletedAt()).isNotNull();
        assertThat(updatedContract.getCompletionStaffId()).isEqualTo(staff.getId());
        assertThat(updatedContract.getEarlyReturnRefundAmount()).isEqualTo(50000);

        // Proposal đổi xe chờ thanh toán phải bị CANCELLED
        var changes = vehicleChanges.findByRentalOrderIdOrderByIdDesc(order.getId());
        assertThat(changes).singleElement().satisfies(c -> assertThat(c.getStatus()).isEqualTo("CANCELLED"));
    }

    @Test
    void security_onlyOwnerCustomerCanRequestEarlyReturn() {
        login(otherCustomer);
        assertThatThrownBy(() -> rentalService.requestEarlyReturn(order.getId(),
                EarlyReturnRequest.builder().reason("Muốn trả xe").build(),
                otherCustomer.getId()))
                .isInstanceOf(Exception.class);
    }

    @Test
    void statusCheck_cannotRequestEarlyReturnBeforeHandover() {
        order.setStatus(RentalStatus.CONFIRMED);
        orders.saveAndFlush(order);

        login(customer);
        assertThatThrownBy(() -> rentalService.requestEarlyReturn(order.getId(),
                EarlyReturnRequest.builder().reason("Chưa nhận đã muốn trả").build(),
                customer.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chỉ có thể yêu cầu trả xe sớm khi đang trong quá trình thuê xe");
    }
}
