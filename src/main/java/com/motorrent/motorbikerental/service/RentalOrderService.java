package com.motorrent.motorbikerental.service;

import com.motorrent.motorbikerental.dto.request.EarlyReturnRequest;
import com.motorrent.motorbikerental.dto.request.CompleteOrderRequest;
import com.motorrent.motorbikerental.dto.request.HandoverRequest;
import com.motorrent.motorbikerental.dto.request.RejectRentalOrderRequest;
import com.motorrent.motorbikerental.dto.request.RentalOrderRequest;
import com.motorrent.motorbikerental.dto.response.RentalOrderResponse;
import com.motorrent.motorbikerental.dto.response.RentalDetailResponse;
import com.motorrent.motorbikerental.entity.Motorbike;
import com.motorrent.motorbikerental.entity.Payment;
import com.motorrent.motorbikerental.entity.RentalOrder;
import com.motorrent.motorbikerental.entity.User;
import com.motorrent.motorbikerental.enums.RentalStatus;
import com.motorrent.motorbikerental.exception.AppException;
import com.motorrent.motorbikerental.exception.ErrorCode;
import com.motorrent.motorbikerental.mapper.RentalOrderMapper;
import com.motorrent.motorbikerental.repository.MotorbikeRepository;
import com.motorrent.motorbikerental.repository.PaymentRepository;
import com.motorrent.motorbikerental.repository.RentalOrderRepository;
import com.motorrent.motorbikerental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentalOrderService {
    private final RentalOrderRepository rentalOrderRepository;
    private final RentalOrderMapper rentalOrderMapper;
    private final MotorbikeRepository motorbikeRepository;
    private final UserRepository userRepository;
    private final com.motorrent.motorbikerental.security.CurrentUser current;
    private final PricingService pricing;
    private final AvailabilityService availability;
    private final com.motorrent.motorbikerental.repository.RentalVehicleChangeRepository vehicleChanges;
    private final NotificationService notifications;
    private final com.motorrent.motorbikerental.repository.RentalContractRepository contracts;
    private final PaymentRepository payments;

    public RentalOrder requireReadable(Long id) {
        var o = rentalOrderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RENTAL_ORDER_NOT_FOUND));
        current.canRead(o);
        return o;
    }

    public RentalOrder requireOwned(Long id) {
        var o = requireReadable(id);
        current.owns(o);
        return o;
    }

    public java.util.Map<String, Object> quote(RentalOrderRequest r) {
        return pricing.quote(r, motorbikeRepository.findById(r.getMotorbikeId())
                .orElseThrow(() -> new AppException(ErrorCode.MOTORBIKE_NOT_FOUND)));
    }

    public List<RentalOrderResponse> getAllOrders() {
        return rentalOrderRepository.findAll().stream()
                .map(rentalOrderMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<RentalOrderResponse> getCustomerOrders(String customerId) {
        return rentalOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(rentalOrderMapper::toResponse)
                .collect(Collectors.toList());
    }

    public List<RentalOrderResponse> getPendingOrders() {
        return rentalOrderRepository.findByStatusOrderByCreatedAtDesc(RentalStatus.PENDING).stream()
                .map(rentalOrderMapper::toResponse)
                .collect(Collectors.toList());
    }

    public RentalDetailResponse getOrderDetail(Long orderId) {
        RentalOrder order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RENTAL_ORDER_NOT_FOUND));
        return rentalOrderMapper.toDetailResponse(order);
    }

    @Transactional
    public RentalOrderResponse createOrder(RentalOrderRequest request, String customerId) {
        Motorbike motorbike = motorbikeRepository.lockById(request.getMotorbikeId())
                .orElseThrow(() -> new AppException(ErrorCode.MOTORBIKE_NOT_FOUND));
        availability.requireAvailable(motorbike, null, request.getPickupDate(), request.getReturnDate());
        var q = pricing.quote(request, motorbike);
        int required = Integer.valueOf(2).equals(request.getSecurityOption()) ? 2 : 1;
        if (!Boolean.TRUE.equals(request.getTermsAccepted()) || request.getPeople() == null
                || request.getPeople().size() != required)
            throw new IllegalArgumentException("Cần đồng ý điều khoản và nhập đủ thông tin giấy tờ");
        for (var person : request.getPeople()) {
            if (person.name() == null || person.name().isBlank() || person.phone() == null
                    || !person.phone().matches("[+0-9 ()-]{9,20}") || person.idCard() == null
                    || !person.idCard().matches("[a-zA-Z0-9]{6,20}") || person.address() == null
                    || person.address().isBlank())
                throw new IllegalArgumentException("Thông tin người thuê chưa đầy đủ hoặc không hợp lệ");
            try {
                if (!java.time.LocalDate.parse(person.birthDate()).isBefore(java.time.LocalDate.now()))
                    throw new IllegalArgumentException();
            } catch (Exception ex) {
                throw new IllegalArgumentException("Ngày sinh không hợp lệ");
            }
        }
        if (required == 2 && request.getPeople().get(0).idCard().equalsIgnoreCase(request.getPeople().get(1).idCard()))
            throw new IllegalArgumentException("Hai giấy tờ phải thuộc hai người khác nhau");
        String identity;
        try {
            identity = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(request.getPeople());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Không lưu được thông tin hợp đồng");
        }
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        int days = (Integer) q.get("days"), totalPrice = (Integer) q.get("totalPrice");
        RentalOrder order = RentalOrder.builder()
                .orderCode("RNT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customer)
                .motorbike(motorbike)
                .pickupDate(request.getPickupDate())
                .returnDate(request.getReturnDate())
                .days(days)
                .totalPrice(totalPrice)
                .discountAmount(0)
                .pickupFee((Integer) q.get("pickupFee")).returnFee((Integer) q.get("returnFee"))
                .pickupLat(request.getPickupLat()).pickupLng(request.getPickupLng()).returnLat(request.getReturnLat())
                .returnLng(request.getReturnLng())
                .rentalFee((Integer) q.get("rentalFee")).deliveryFee((Integer) q.get("deliveryFee"))
                .securityDeposit((Integer) q.get("securityDeposit"))
                .dailyRate(motorbike.getPricePerDay()).depositDue((Integer) q.get("depositDue")).balancePaid(0)
                .returnType(request.getReturnType() == null ? "STORE" : request.getReturnType())
                .returnAddress(request.getReturnAddress())
                .paymentExpiresAt(LocalDateTime.now().plusMinutes((Integer) q.get("holdMinutes")))
                .pickupType(request.getPickupType() != null ? request.getPickupType() : "STORE")
                .pickupAddress(request.getPickupAddress())
                .note(request.getNote()).identitySnapshot(identity).termsAccepted(true)
                .status(RentalStatus.PENDING_PAYMENT)
                .depositPaid(0)
                .build();

        RentalOrder savedOrder = rentalOrderRepository.save(order);
        notifications.orderChanged(savedOrder);
        return rentalOrderMapper.toResponse(savedOrder);
    }

    @Transactional
    public RentalOrderResponse confirmOrder(Long orderId, String staffId) {
        RentalOrder order = rentalOrderRepository.lockById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RENTAL_ORDER_NOT_FOUND));

        if (order.getStatus() != RentalStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_RENTAL_STATUS);
        }

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        order.setStatus(RentalStatus.CONFIRMED);
        order.setStaff(staff);

        notifications.orderChanged(order);
        return rentalOrderMapper.toResponse(rentalOrderRepository.save(order));
    }

    @Transactional
    public RentalOrderResponse rejectOrder(Long orderId, RejectRentalOrderRequest request, String staffId) {
        RentalOrder order = rentalOrderRepository.lockById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RENTAL_ORDER_NOT_FOUND));

        if (order.getStatus() != RentalStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_RENTAL_STATUS);
        }

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (java.util.Objects.requireNonNullElse(order.getDepositPaid(), 0)
                + java.util.Objects.requireNonNullElse(order.getBalancePaid(), 0) > 0)
            throw new IllegalArgumentException("Đơn đã nhận tiền; dùng xác nhận đã hoàn tiền để từ chối đơn");
        order.setStatus(RentalStatus.REJECTED);
        order.setStaff(staff);
        order.setRejectionReason(request.getReason());

        notifications.orderChanged(order);
        return rentalOrderMapper.toResponse(rentalOrderRepository.save(order));
    }

    @Transactional
    public RentalOrderResponse handOverOrder(Long orderId, String staffId) {
        return handOverOrder(orderId, staffId, null);
    }

    @Transactional
    public RentalOrderResponse handOverOrder(Long orderId, String staffId, HandoverRequest request) {
        RentalOrder order = availability.lockOrder(orderId);

        if (order.getStatus() != RentalStatus.READY_FOR_HANDOVER && order.getStatus() != RentalStatus.CONFIRMED) {
            throw new AppException(ErrorCode.INVALID_RENTAL_STATUS);
        }

        if (vehicleChanges.lockPending(orderId).stream()
                .anyMatch(c -> c.getExpiresAt().isAfter(AvailabilityService.now()))) {
            throw new IllegalArgumentException("Hoàn tất hoặc hủy đề nghị đổi xe trước khi bàn giao");
        }

        var existingPayments = payments.lockForOrder(orderId);
        var now = AvailabilityService.now();
        var actualPickup = (request != null && request.getActualPickupTime() != null) ? request.getActualPickupTime()
                : now;

        if (order.getReturnDate() != null && !actualPickup.isBefore(order.getReturnDate())) {
            throw new IllegalArgumentException("Thời điểm nhận xe phải trước thời điểm trả dự kiến");
        }

        availability.requireAvailable(order.getMotorbike(), orderId, actualPickup, order.getReturnDate());
        if (order.getMotorbike().getStatus() != com.motorrent.motorbikerental.enums.MotorbikeStatus.AVAILABLE) {
            throw new IllegalArgumentException("Xe chưa sẵn sàng để bàn giao thực tế");
        }

        User staff = userRepository.findById(staffId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 1. Remaining rental collection
        int requiredRemaining = order.getRemainingRentalAmount();
        int collectedRemaining;
        if (requiredRemaining > 0) {
            if (request == null || request.getRemainingAmount() == null) {
                collectedRemaining = requiredRemaining;
            } else {
                int given = request.getRemainingAmount();
                if (given < 0)
                    throw new IllegalArgumentException("Số tiền thuê còn lại không hợp lệ");
                if (given < requiredRemaining)
                    throw new IllegalArgumentException("Chưa thu đủ tiền thuê còn lại");
                if (given > requiredRemaining)
                    throw new IllegalArgumentException("Số tiền thuê vượt mức cần thanh toán");
                collectedRemaining = given;
            }
        } else {
            if (request != null && request.getRemainingAmount() != null && request.getRemainingAmount() > 0) {
                throw new IllegalArgumentException("Đơn đã thanh toán đủ tiền thuê");
            }
            collectedRemaining = 0;
        }

        // 2. Asset deposit collection
        boolean depositAlreadyPaid = existingPayments.stream()
                .anyMatch(p -> Financials.posted(p) && "ASSET_DEPOSIT".equals(Financials.type(p)));
        int requiredDeposit = depositAlreadyPaid ? 0 : Objects.requireNonNullElse(order.getSecurityDeposit(), 0);
        int collectedDeposit;
        if (requiredDeposit > 0) {
            if (request == null || request.getSecurityDeposit() == null) {
                collectedDeposit = requiredDeposit;
            } else {
                int given = request.getSecurityDeposit();
                if (given < 0)
                    throw new IllegalArgumentException("Tiền cọc tài sản không hợp lệ");
                if (given < requiredDeposit)
                    throw new IllegalArgumentException("Chưa thu đủ tiền cọc tài sản");
                if (given > requiredDeposit)
                    throw new IllegalArgumentException("Tiền cọc tài sản vượt mức quy định");
                collectedDeposit = given;
            }
        } else {
            if (request != null && request.getSecurityDeposit() != null && request.getSecurityDeposit() > 0) {
                throw new IllegalArgumentException("Đơn không yêu cầu thu cọc tài sản");
            }
            collectedDeposit = 0;
        }

        // 3. Early pickup fee
        int earlyFee = (request != null && request.getEarlyPickupFee() != null) ? request.getEarlyPickupFee() : 0;
        if (earlyFee < 0)
            throw new IllegalArgumentException("Phí nhận sớm không hợp lệ");
        if (earlyFee > 0 && order.getPickupDate() != null && !actualPickup.isBefore(order.getPickupDate())) {
            throw new IllegalArgumentException("Không thể thu phí nhận sớm khi không nhận xe sớm");
        }

        // 4. Payment method & note
        String method = (request != null && request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank())
                ? request.getPaymentMethod().trim()
                : "CASH";
        if (!List.of("CASH", "BANK_TRANSFER", "OTHER").contains(method)) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");
        }
        String note = (request != null) ? request.getNote() : null;
        if (note != null && note.length() > 2000) {
            throw new IllegalArgumentException("Ghi chú tối đa 2000 ký tự");
        }

        long ts = System.currentTimeMillis();
        if (collectedRemaining > 0) {
            order.setBalancePaid(Objects.requireNonNullElse(order.getBalancePaid(), 0) + collectedRemaining);
            payments.save(Payment.builder()
                    .rentalOrder(order)
                    .amount(collectedRemaining)
                    .receivedAmount(collectedRemaining)
                    .direction("IN")
                    .purpose("BALANCE")
                    .transactionType("RENTAL_REMAINING")
                    .paymentMethod(method)
                    .status("PAID")
                    .paidAt(now)
                    .processedAt(now)
                    .processedByStaffId(staffId)
                    .sourceType("HANDOVER_COLLECTION")
                    .sourceId(order.getId())
                    .transactionCode(method + "-REM-" + order.getOrderCode() + "-" + ts)
                    .idempotencyKey("HANDOVER:REM:" + order.getId())
                    .note(note)
                    .componentsJson(
                            Financials.components(Map.of("RENTAL_REMAINING", collectedRemaining), collectedRemaining))
                    .build());
        }

        if (collectedDeposit > 0) {
            payments.save(Payment.builder()
                    .rentalOrder(order)
                    .amount(collectedDeposit)
                    .receivedAmount(collectedDeposit)
                    .direction("IN")
                    .purpose("SECURITY")
                    .transactionType("ASSET_DEPOSIT")
                    .paymentMethod(method)
                    .status("PAID")
                    .paidAt(now)
                    .processedAt(now)
                    .processedByStaffId(staffId)
                    .sourceType("HANDOVER_COLLECTION")
                    .sourceId(order.getId())
                    .transactionCode(method + "-SEC-" + order.getOrderCode() + "-" + ts)
                    .idempotencyKey("HANDOVER:SEC:" + order.getId())
                    .note(note)
                    .componentsJson(Financials.components(Map.of("ASSET_DEPOSIT", collectedDeposit), collectedDeposit))
                    .build());
        }

        if (earlyFee > 0) {
            payments.save(Payment.builder()
                    .rentalOrder(order)
                    .amount(earlyFee)
                    .receivedAmount(earlyFee)
                    .direction("IN")
                    .purpose("OTHER")
                    .transactionType("EARLY_PICKUP_FEE")
                    .paymentMethod(method)
                    .status("PAID")
                    .paidAt(now)
                    .processedAt(now)
                    .processedByStaffId(staffId)
                    .sourceType("HANDOVER_COLLECTION")
                    .sourceId(order.getId())
                    .transactionCode(method + "-EARLY-" + order.getOrderCode() + "-" + ts)
                    .idempotencyKey("HANDOVER:EARLY:" + order.getId())
                    .note(note)
                    .componentsJson(Financials.components(Map.of("EARLY_PICKUP_FEE", earlyFee), earlyFee))
                    .build());
        }

        var contract = contracts.findByRentalOrderId(orderId)
                .orElseGet(() -> com.motorrent.motorbikerental.entity.RentalContract.builder()
                        .rentalOrder(order).contractCode("HD-" + order.getOrderCode())
                        .odoBefore(order.getMotorbike().getOdoKm()).build());
        contract.setHandedOverAt(actualPickup);
        contract.setHandoverStaffId(staffId);
        contracts.save(contract);

        order.setStatus(RentalStatus.HANDED_OVER);

        order.getMotorbike().setStatus(com.motorrent.motorbikerental.enums.MotorbikeStatus.RENTED);
        order.setStaff(staff);

        notifications.orderChanged(order);
        return rentalOrderMapper.toResponse(rentalOrderRepository.save(order));
    }

    @Transactional
    public RentalOrderResponse requestEarlyReturn(Long orderId, EarlyReturnRequest request, String customerId) {
        RentalOrder order = availability.lockOrder(orderId);
        if (customerId != null && !Objects.equals(order.getCustomer().getId(), customerId)) {
            try {
                current.operator();
            } catch (Exception e) {
                throw new AppException(ErrorCode.PERMISSION_DENIED);
            }
        }
        if (order.getStatus() != RentalStatus.HANDED_OVER && order.getStatus() != RentalStatus.EXTENDED) {
            throw new IllegalArgumentException("Chỉ có thể yêu cầu trả xe sớm khi đang trong quá trình thuê xe");
        }

        var contract = contracts.findByRentalOrderId(orderId)
                .orElseGet(() -> com.motorrent.motorbikerental.entity.RentalContract.builder()
                        .rentalOrder(order).contractCode("HD-" + order.getOrderCode()).build());

        var now = LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        contract.setEarlyReturnRequestedAt(now);
        if (request != null && request.getReason() != null && !request.getReason().isBlank()) {
            contract.setEarlyReturnReason(request.getReason().trim());
        }
        contracts.save(contract);

        order.setStatus(RentalStatus.RETURN_REQUESTED);

        notifications.orderChanged(order);
        return rentalOrderMapper.toResponse(rentalOrderRepository.save(order));
    }

    @Transactional
    public RentalOrderResponse completeOrder(Long orderId, String staffId) {
        return completeOrder(orderId, null, staffId);
    }

    @Transactional
    public RentalOrderResponse completeOrder(Long orderId, CompleteOrderRequest request, String staffId) {
        var bikeIds = new java.util.TreeSet<Long>();
        var preCheck = rentalOrderRepository.findById(orderId).orElseThrow(() -> new AppException(ErrorCode.RENTAL_ORDER_NOT_FOUND));
        if (preCheck.getMotorbike() != null && preCheck.getMotorbike().getId() != null) {
            bikeIds.add(preCheck.getMotorbike().getId());
        }
        for (Long bId : bikeIds) {
            motorbikeRepository.lockById(bId).orElseThrow();
        }

        RentalOrder order = availability.lockOrder(orderId);
        var changes = vehicleChanges.findByRentalOrderIdOrderByIdDesc(orderId);
        if (changes.stream().anyMatch(c -> "READY_FOR_EXCHANGE".equals(c.getStatus()))) {
            throw new IllegalArgumentException("Phương án đổi xe đã thanh toán chưa thực hiện; cần xử lý phương án trước khi hoàn tất đơn");
        }
        for (var c : changes) {
            if ("WAITING_FOR_PAYMENT".equals(c.getStatus()) || "PENDING".equals(c.getStatus())) {
                c.setStatus("CANCELLED");
                vehicleChanges.save(c);
            }
        }

        if (!java.util.List.of(RentalStatus.HANDED_OVER, RentalStatus.EXTENDED, RentalStatus.OVERDUE,
                RentalStatus.RETURN_REQUESTED, RentalStatus.RETURNED).contains(order.getStatus())) {
            throw new AppException(ErrorCode.INVALID_RENTAL_STATUS);
        }

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        int refundAmount = (request != null && request.getRefundAmount() != null) ? request.getRefundAmount() : 0;
        if (refundAmount < 0) {
            throw new IllegalArgumentException("Số tiền hoàn không được âm");
        }

        var orderPayments = payments.lockForOrder(orderId);
        int rentalPaid = 0;
        int rentalRefunded = 0;
        for (var p : orderPayments) {
            if (Financials.posted(p)) {
                String type = Financials.type(p);
                if (List.of("BOOKING_PAYMENT", "RENTAL_REMAINING", "EXTENSION_PAYMENT").contains(type)) {
                    rentalPaid += Math.abs(p.getAmount());
                } else if (List.of("BOOKING_REFUND", "BOOKING_ADJUSTMENT_REFUND", "EARLY_RETURN_REFUND").contains(type)) {
                    rentalRefunded += Math.abs(p.getAmount());
                }
            }
        }
        int netRentalPaid = Math.max(0, rentalPaid - rentalRefunded);

        if (refundAmount > netRentalPaid) {
            throw new IllegalArgumentException("Số tiền hoàn trả sớm (" + refundAmount + " đ) không được vượt quá số tiền thuê khách đã thanh toán (" + netRentalPaid + " đ)");
        }

        var now = LocalDateTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        String method = (request != null && request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank())
                ? request.getPaymentMethod().trim() : "CASH";
        if (!List.of("CASH", "BANK_TRANSFER", "OTHER").contains(method)) {
            throw new IllegalArgumentException("Phương thức hoàn tiền không hợp lệ (chỉ chấp nhận CASH, BANK_TRANSFER, OTHER)");
        }

        String reason = (request != null && request.getRefundReason() != null && !request.getRefundReason().isBlank())
                ? request.getRefundReason().trim() : null;

        if (refundAmount > 0) {
            if (reason == null || reason.isBlank()) {
                reason = "Hoàn tiền trả xe sớm";
            }

            String idempotencyKey = "EARLY_RETURN_REFUND:" + orderId;
            boolean alreadyRefunded = orderPayments.stream()
                    .anyMatch(p -> idempotencyKey.equals(p.getIdempotencyKey()) && Financials.posted(p));

            if (!alreadyRefunded) {
                String note = (request != null && request.getNote() != null && !request.getNote().isBlank())
                        ? reason + " - " + request.getNote().trim() : reason;
                if (note.length() > 2000) note = note.substring(0, 2000);

                Payment refundPayment = Payment.builder()
                        .rentalOrder(order)
                        .amount(refundAmount)
                        .receivedAmount(refundAmount)
                        .direction("OUT")
                        .purpose("EARLY_RETURN_REFUND")
                        .transactionType("EARLY_RETURN_REFUND")
                        .paymentMethod(method)
                        .status("PAID")
                        .paidAt(now)
                        .processedAt(now)
                        .processedByStaffId(staffId)
                        .transactionCode("REFUND-" + order.getOrderCode() + "-" + System.currentTimeMillis() % 100000)
                        .idempotencyKey(idempotencyKey)
                        .sourceType("EARLY_RETURN")
                        .sourceId(orderId)
                        .note(note)
                        .componentsJson(Financials.components(Map.of("EARLY_RETURN_REFUND", refundAmount), refundAmount))
                        .build();
                payments.save(refundPayment);
            }
        }

        // Phase 7: Asset deposit reconciliation (Cases 52, 53, 54, 55)
        int assetDeposited = 0;
        int assetRefunded = 0;
        int assetDeducted = 0;
        for (var p : orderPayments) {
            if (Financials.posted(p)) {
                String type = Financials.type(p);
                if (List.of("ASSET_DEPOSIT", "ASSET_DEPOSIT_ADDITIONAL").contains(type)) {
                    assetDeposited += Math.abs(p.getAmount());
                } else if ("ASSET_DEPOSIT_REFUND".equals(type)) {
                    assetRefunded += Math.abs(p.getAmount());
                } else if ("DEPOSIT_DEDUCTION".equals(type)) {
                    assetDeducted += Math.abs(p.getAmount());
                }
            }
        }
        int heldAssetDeposit = Math.max(0, assetDeposited - assetRefunded - assetDeducted);

        Integer reqDepositRefund = request != null ? request.getAssetDepositRefundAmount() : null;
        Integer reqDepositDeduct = request != null ? request.getAssetDepositDeductionAmount() : null;
        String depositDeductReason = (request != null && request.getDepositDeductionReason() != null)
                ? request.getDepositDeductionReason().trim() : null;
        String depositRefundMethod = (request != null && request.getDepositRefundMethod() != null && !request.getDepositRefundMethod().isBlank())
                ? request.getDepositRefundMethod().trim() : "CASH";
        if (!List.of("CASH", "BANK_TRANSFER", "OTHER").contains(depositRefundMethod)) {
            throw new IllegalArgumentException("Phương thức hoàn cọc không hợp lệ (chỉ chấp nhận CASH, BANK_TRANSFER, OTHER)");
        }

        int depositRefundAmount = 0;
        int depositDeductionAmount = 0;

        if (heldAssetDeposit > 0) {
            if (reqDepositRefund == null && reqDepositDeduct == null) {
                depositRefundAmount = heldAssetDeposit;
                depositDeductionAmount = 0;
            } else {
                depositRefundAmount = reqDepositRefund != null ? reqDepositRefund : 0;
                depositDeductionAmount = reqDepositDeduct != null ? reqDepositDeduct : 0;
                if (depositRefundAmount < 0 || depositDeductionAmount < 0) {
                    throw new IllegalArgumentException("Số tiền hoàn cọc hoặc khấu trừ cọc không được âm");
                }
                if (depositRefundAmount + depositDeductionAmount != heldAssetDeposit) {
                    throw new IllegalArgumentException("Tổng tiền hoàn cọc (" + depositRefundAmount + " đ) và khấu trừ cọc (" + depositDeductionAmount + " đ) phải bằng tổng tiền cọc đang giữ (" + heldAssetDeposit + " đ)");
                }
                if (depositDeductionAmount > 0 && (depositDeductReason == null || depositDeductReason.isBlank())) {
                    throw new IllegalArgumentException("Phải nhập lý do khấu trừ cọc");
                }
            }
        } else {
            if ((reqDepositRefund != null && reqDepositRefund > 0) || (reqDepositDeduct != null && reqDepositDeduct > 0)) {
                throw new IllegalArgumentException("Đơn không có khoản cọc tài sản đang giữ để hoàn hoặc khấu trừ");
            }
        }

        if (depositRefundAmount > 0) {
            String depRefIdempotency = "ASSET_DEPOSIT_REFUND:" + orderId;
            boolean alreadyDepRef = orderPayments.stream()
                    .anyMatch(p -> depRefIdempotency.equals(p.getIdempotencyKey()) && Financials.posted(p));
            if (!alreadyDepRef) {
                Payment depRefPayment = Payment.builder()
                        .rentalOrder(order)
                        .amount(depositRefundAmount)
                        .receivedAmount(depositRefundAmount)
                        .direction("OUT")
                        .purpose("SECURITY_REFUND")
                        .transactionType("ASSET_DEPOSIT_REFUND")
                        .paymentMethod(depositRefundMethod)
                        .status("PAID")
                        .paidAt(now)
                        .processedAt(now)
                        .processedByStaffId(staffId)
                        .transactionCode("DEPREF-" + order.getOrderCode() + "-" + System.currentTimeMillis() % 100000)
                        .idempotencyKey(depRefIdempotency)
                        .sourceType("COMPLETION")
                        .sourceId(orderId)
                        .note("Hoàn cọc tài sản khi hoàn tất đơn")
                        .componentsJson(Financials.components(Map.of("ASSET_DEPOSIT_REFUND", depositRefundAmount), depositRefundAmount))
                        .build();
                payments.save(depRefPayment);
            }
        }

        if (depositDeductionAmount > 0) {
            String depDedIdempotency = "DEPOSIT_DEDUCTION:" + orderId;
            boolean alreadyDepDed = orderPayments.stream()
                    .anyMatch(p -> depDedIdempotency.equals(p.getIdempotencyKey()) && Financials.posted(p));
            if (!alreadyDepDed) {
                Payment depDedPayment = Payment.builder()
                        .rentalOrder(order)
                        .amount(depositDeductionAmount)
                        .receivedAmount(depositDeductionAmount)
                        .direction("OUT")
                        .purpose("DEPOSIT_DEDUCTION")
                        .transactionType("DEPOSIT_DEDUCTION")
                        .paymentMethod("DEPOSIT_DEDUCTION")
                        .status("PAID")
                        .paidAt(now)
                        .processedAt(now)
                        .processedByStaffId(staffId)
                        .transactionCode("DEPDED-" + order.getOrderCode() + "-" + System.currentTimeMillis() % 100000)
                        .idempotencyKey(depDedIdempotency)
                        .sourceType("COMPLETION")
                        .sourceId(orderId)
                        .note(depositDeductReason)
                        .componentsJson(Financials.components(Map.of("DEPOSIT_DEDUCTION", depositDeductionAmount), depositDeductionAmount))
                        .build();
                payments.save(depDedPayment);
            }
        }

        // Phase 7: Lateness & late fee (Cases 56, 57, 58)
        Integer reqLateFee = request != null ? request.getLateFee() : null;
        int lateFee = reqLateFee != null ? reqLateFee : 0;
        if (lateFee < 0) {
            throw new IllegalArgumentException("Phí trả trễ không được âm");
        }
        String lateFeeReason = (request != null && request.getLateFeeReason() != null)
                ? request.getLateFeeReason().trim() : null;
        String lateFeeMethod = (request != null && request.getLateFeePaymentMethod() != null && !request.getLateFeePaymentMethod().isBlank())
                ? request.getLateFeePaymentMethod().trim() : "CASH";
        if (!List.of("CASH", "BANK_TRANSFER", "OTHER").contains(lateFeeMethod)) {
            throw new IllegalArgumentException("Phương thức thu phí trễ không hợp lệ (chỉ chấp nhận CASH, BANK_TRANSFER, OTHER)");
        }

        if (lateFee > 0) {
            if (lateFeeReason == null || lateFeeReason.isBlank()) {
                lateFeeReason = "Phí trả xe trễ";
            }
            String lateFeeIdempotency = "LATE_FEE:" + orderId;
            boolean alreadyLateFee = orderPayments.stream()
                    .anyMatch(p -> lateFeeIdempotency.equals(p.getIdempotencyKey()) && Financials.posted(p));
            if (!alreadyLateFee) {
                Payment latePayment = Payment.builder()
                        .rentalOrder(order)
                        .amount(lateFee)
                        .receivedAmount(lateFee)
                        .direction("IN")
                        .purpose("LATE_FEE")
                        .transactionType("LATE_FEE")
                        .paymentMethod(lateFeeMethod)
                        .status("PAID")
                        .paidAt(now)
                        .processedAt(now)
                        .processedByStaffId(staffId)
                        .transactionCode("LATE-" + order.getOrderCode() + "-" + System.currentTimeMillis() % 100000)
                        .idempotencyKey(lateFeeIdempotency)
                        .sourceType("COMPLETION")
                        .sourceId(orderId)
                        .note(lateFeeReason)
                        .componentsJson(Financials.components(Map.of("LATE_FEE", lateFee), lateFee))
                        .build();
                payments.save(latePayment);
            }
        }

        var contract = contracts.findByRentalOrderId(orderId)
                .orElseGet(() -> com.motorrent.motorbikerental.entity.RentalContract.builder()
                        .rentalOrder(order).contractCode("HD-" + order.getOrderCode()).build());
        contract.setCompletedAt(now);
        contract.setCompletionStaffId(staffId);
        contract.setEarlyReturnRefundAmount(refundAmount);
        contract.setEarlyReturnDecisionReason(reason);
        contract.setEarlyReturnPaymentMethod(method);
        contract.setAssetDepositHeld(heldAssetDeposit);
        contract.setAssetDepositRefundAmount(depositRefundAmount);
        contract.setAssetDepositDeductionAmount(depositDeductionAmount);
        contract.setDepositDeductionReason(depositDeductReason);
        contract.setLateFee(lateFee);
        contract.setLateFeeReason(lateFeeReason);
        contract.setLateFeePaymentMethod(lateFeeMethod);
        contracts.save(contract);

        order.getMotorbike().setStatus(com.motorrent.motorbikerental.enums.MotorbikeStatus.AVAILABLE);
        order.setStatus(RentalStatus.COMPLETED);
        order.setActualReturnDate(now);
        order.setStaff(staff);

        notifications.orderChanged(order);
        return rentalOrderMapper.toResponse(rentalOrderRepository.save(order));
    }

    @Transactional
    public RentalOrderResponse cancelOrder(Long orderId) {
        RentalOrder order = rentalOrderRepository.lockById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RENTAL_ORDER_NOT_FOUND));

        if (order.getStatus() != RentalStatus.PENDING && order.getStatus() != RentalStatus.PENDING_PAYMENT) {
            throw new RuntimeException("Chỉ có thể hủy đơn khi đang chờ xác nhận");
        }

        order.setStatus(RentalStatus.CANCELLED);

        notifications.orderChanged(order);
        return rentalOrderMapper.toResponse(rentalOrderRepository.save(order));
    }

    @Transactional
    public RentalOrderResponse rejectOrder(Long orderId, String staffId, String reason) {
        if (reason == null || reason.isBlank())
            throw new IllegalArgumentException("Nhập lý do từ chối");
        var request = new RejectRentalOrderRequest();
        request.setReason(reason.trim());
        return rejectOrder(orderId, request, staffId);
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupExpiredOrders() {
        java.util.List<RentalOrder> expiredOrders = rentalOrderRepository.findAll().stream()
                .filter(o -> o.getStatus() == RentalStatus.PENDING_PAYMENT && o.getPaymentExpiresAt() != null
                        && o.getPaymentExpiresAt().isBefore(AvailabilityService.now()))
                .collect(java.util.stream.Collectors.toList());

        for (RentalOrder candidate : expiredOrders) {
            RentalOrder order = rentalOrderRepository.lockById(candidate.getId()).orElseThrow();
            if (order.getStatus() != RentalStatus.PENDING_PAYMENT || order.getPaymentExpiresAt() == null
                    || !order.getPaymentExpiresAt().isBefore(AvailabilityService.now()))
                continue;
            order.setStatus(RentalStatus.CANCELLED);
            order.setRejectionReason("Hết hạn thanh toán cọc");
            rentalOrderRepository.save(order);
            notifications.orderChanged(order);
        }
    }
}
