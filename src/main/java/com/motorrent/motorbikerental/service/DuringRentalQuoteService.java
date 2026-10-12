package com.motorrent.motorbikerental.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.motorrent.motorbikerental.dto.request.DuringRentalQuoteRequest;
import com.motorrent.motorbikerental.entity.RentalOrder;
import com.motorrent.motorbikerental.enums.RentalStatus;
import com.motorrent.motorbikerental.repository.*;
import com.motorrent.motorbikerental.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/** Advisory quotation only. A future commit must recheck schedule and ledger under locks. */
@Service
@RequiredArgsConstructor
public class DuringRentalQuoteService {
    private final CurrentUser current;
    private final RentalOrderRepository orders;
    private final MotorbikeRepository bikes;
    private final PaymentRepository payments;
    private final AvailabilityService availability;

    private RentalOrder activeOrder(Long id, LocalDateTime start, LocalDateTime end) {
        current.operator();
        var order = orders.findById(id).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn thuê"));
        if (!List.of(RentalStatus.HANDED_OVER, RentalStatus.EXTENDED, RentalStatus.OVERDUE).contains(order.getStatus()))
            throw new IllegalArgumentException("Chỉ lập phương án đổi xe khi khách đang thuê");
        PricingService.validateStart(start);
        PricingService.days(start, end); // Validates range only; never rounds negotiated charges.
        if (order.getPickupDate() == null || start.isBefore(order.getPickupDate()))
            throw new IllegalArgumentException("Giờ đổi xe phải sau giờ bắt đầu thuê");
        if (order.getReturnDate() == null || !start.equals(order.getReturnDate()))
            throw new IllegalArgumentException("Hiện hỗ trợ đổi xe tại cuối chặng đã thanh toán; đổi giữa chặng cần chốt quy tắc giá trước");
        return order;
    }

    private boolean twoDocuments(RentalOrder order) {
        try {
            var identities = new ObjectMapper().readTree(order.getIdentitySnapshot());
            if (identities != null && identities.isArray()) return identities.size() == 2;
        } catch (Exception ignored) { /* Legacy fallback matches BEFORE_DELIVERY. */ }
        return Integer.valueOf(0).equals(order.getSecurityDeposit());
    }

    @Transactional(readOnly = true)
    public List<Map<String,Object>> options(Long id, LocalDateTime start, LocalDateTime end) {
        var order = activeOrder(id, start, end);
        int held = AssetDepositAmounts.held(payments.findByRentalOrderIdOrderByCreatedAtDesc(id));
        boolean two = twoDocuments(order);
        var result = new ArrayList<Map<String,Object>>();
        for (var bike : bikes.findAll()) {
            if (bike.getId().equals(order.getMotorbike().getId()) || !availability.available(bike, id, start, end)) continue;
            var deposit = AssetDepositAmounts.quote(two ? 0 : PricingService.securityDeposit(bike), held);
            result.add(OperationsService.row("motorbikeId", bike.getId(), "name", bike.getName(),
                    "licensePlate", bike.getLicensePlate(), "image", bike.getImagePrimary(),
                    "dailyRate", bike.getPricePerDay(), "category", bike.getCategory() == null ? "" : bike.getCategory().getName(),
                    "requiredDeposit", deposit.requiredDeposit(), "heldDeposit", held,
                    "additionalDeposit", deposit.additionalDeposit(), "heldAfterCollection", deposit.heldAfterCollection()));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String,Object> quote(Long id, DuringRentalQuoteRequest request) {
        current.operator();
        if (request == null || request.motorbikeId() == null) throw new IllegalArgumentException("Chọn xe thay thế");
        var order = activeOrder(id, request.plannedChangeTime(), request.newReturnTime());
        if (request.motorbikeId().equals(order.getMotorbike().getId())) throw new IllegalArgumentException("Chọn xe khác xe hiện tại");
        var target = bikes.findById(request.motorbikeId()).orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xe"));
        if (!availability.available(target, id, request.plannedChangeTime(), request.newReturnTime()))
            throw new IllegalArgumentException("Xe thay thế không còn trống trong toàn bộ khoảng thuê tiếp");
        text(request.reason(), "lý do", 1000);
        text(request.exchangeAddress(), "địa điểm đổi xe", 500);
        int future = Math.multiplyExact(PricingService.days(request.plannedChangeTime(), request.newReturnTime()), target.getPricePerDay());
        int credit = 0;
        int fee = amount(request.vehicleChangeFee());
        if (order.getRentalFee() == null || order.getTotalPrice() == null)
            throw new IllegalArgumentException("Đơn cũ thiếu chi tiết tiền thuê, cần đối soát trước khi lập phương án");
        if (credit > order.getRentalFee()) throw new IllegalArgumentException("Phần tiền thuê chưa dùng không được vượt tiền thuê của đơn");
        if (credit > 0 && !request.plannedChangeTime().isBefore(order.getReturnDate()))
            throw new IllegalArgumentException("Đã hết khoảng thuê cũ nên không có tiền thuê chưa sử dụng để bù trừ");
        int retainedRental = Math.subtractExact(order.getRentalFee(), credit);
        int newRental = Math.addExact(retainedRental, future);
        int newTotal = Math.addExact(Math.addExact(Math.subtractExact(order.getTotalPrice(), credit), future), fee);
        if (future <= 0) throw new IllegalArgumentException("Giá xe thay thế chưa hợp lệ");
        int paid = Math.addExact(Objects.requireNonNullElse(order.getDepositPaid(), 0), Objects.requireNonNullElse(order.getBalancePaid(), 0));
        if (paid != order.getTotalPrice()) throw new IllegalArgumentException("Đối soát đủ tiền chặng cũ trước khi tiếp tục thuê xe khác");
        int additional = Math.addExact(future, fee);
        int held = AssetDepositAmounts.held(payments.findByRentalOrderIdOrderByCreatedAtDesc(id));
        var deposit = AssetDepositAmounts.quote(twoDocuments(order) ? 0 : PricingService.securityDeposit(target), held);
        return OperationsService.row("orderId", id, "type", "DURING_RENTAL", "oldVehicleId", order.getMotorbike().getId(),
                "newVehicleId", target.getId(), "newVehicle", target.getName(), "newDailyRate", target.getPricePerDay(),
                "plannedChangeTime", request.plannedChangeTime(), "newReturnTime", request.newReturnTime(),
                "futureRentalAmount", future, "unusedRentalCredit", credit, "retainedRentalAmount", retainedRental,
                "vehicleChangeFee", fee, "oldTotal", order.getTotalPrice(), "newRentalFee", newRental, "newTotal", newTotal,
                "paidBefore", paid, "additionalPayment", additional, "rentalCreditAmount", Math.max(0, paid-newTotal),
                "requiredDeposit", deposit.requiredDeposit(), "heldDeposit", held, "additionalDeposit", deposit.additionalDeposit(),
                "heldAfterCollection", deposit.heldAfterCollection(), "exchangeAddress", request.exchangeAddress().trim(),
                "reason", request.reason().trim());
    }

    private static int amount(Integer value) {
        if (value == null || value < 0) throw new IllegalArgumentException("Nhập đầy đủ các khoản tiền thỏa thuận, không dùng số âm");
        return value;
    }
    private static void text(String value, String name, int max) {
        if (value == null || value.isBlank() || value.length() > max)
            throw new IllegalArgumentException("Nhập " + name + ", tối đa " + max + " ký tự");
    }
}
