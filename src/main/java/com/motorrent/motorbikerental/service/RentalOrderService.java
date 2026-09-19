package com.motorrent.motorbikerental.service;

import com.motorrent.motorbikerental.dto.request.RejectRentalOrderRequest;
import com.motorrent.motorbikerental.dto.request.RentalOrderRequest;
import com.motorrent.motorbikerental.dto.response.RentalOrderResponse;
import com.motorrent.motorbikerental.dto.response.RentalDetailResponse;
import com.motorrent.motorbikerental.entity.Motorbike;
import com.motorrent.motorbikerental.entity.RentalOrder;
import com.motorrent.motorbikerental.entity.User;
import com.motorrent.motorbikerental.enums.RentalStatus;
import com.motorrent.motorbikerental.exception.AppException;
import com.motorrent.motorbikerental.exception.ErrorCode;
import com.motorrent.motorbikerental.mapper.RentalOrderMapper;
import com.motorrent.motorbikerental.repository.MotorbikeRepository;
import com.motorrent.motorbikerental.repository.RentalOrderRepository;
import com.motorrent.motorbikerental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RentalOrderService {
    private final RentalOrderRepository rentalOrderRepository;
    private final RentalOrderMapper rentalOrderMapper;
    private final MotorbikeRepository motorbikeRepository;
    private final UserRepository userRepository;

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
        if (request.getPickupDate().isAfter(request.getReturnDate()) || request.getPickupDate().isEqual(request.getReturnDate())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        if (rentalOrderRepository.existsOverlappingOrder(request.getMotorbikeId(), request.getPickupDate(), request.getReturnDate())) {
            throw new AppException(ErrorCode.MOTORBIKE_NOT_AVAILABLE);
        }

        Motorbike motorbike = motorbikeRepository.findById(request.getMotorbikeId())
                .orElseThrow(() -> new AppException(ErrorCode.MOTORBIKE_NOT_FOUND));

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        long hours = ChronoUnit.HOURS.between(request.getPickupDate(), request.getReturnDate());
        int days = (int) Math.ceil(hours / 24.0);
        if (days <= 0) days = 1;

        int totalPrice = days * motorbike.getPricePerDay();

        RentalOrder order = RentalOrder.builder()
                .orderCode("RNT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customer)
                .motorbike(motorbike)
                .pickupDate(request.getPickupDate())
                .returnDate(request.getReturnDate())
                .days(days)
                .totalPrice(totalPrice)
                .pickupType(request.getPickupType() != null ? request.getPickupType() : "STORE")
                .pickupAddress(request.getPickupAddress())
                .note(request.getNote())
                .status(RentalStatus.PENDING)
                .depositPaid(0)
                .build();

        RentalOrder savedOrder = rentalOrderRepository.save(order);
        return rentalOrderMapper.toResponse(savedOrder);
    }

    @Transactional
    public RentalOrderResponse confirmOrder(Long orderId, String staffId) {
        RentalOrder order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RENTAL_ORDER_NOT_FOUND));

        if (order.getStatus() != RentalStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_RENTAL_STATUS);
        }

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        order.setStatus(RentalStatus.CONFIRMED);
        order.setStaff(staff);
        
        return rentalOrderMapper.toResponse(rentalOrderRepository.save(order));
    }

    @Transactional
    public RentalOrderResponse rejectOrder(Long orderId, RejectRentalOrderRequest request, String staffId) {
        RentalOrder order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RENTAL_ORDER_NOT_FOUND));

        if (order.getStatus() != RentalStatus.PENDING) {
            throw new AppException(ErrorCode.INVALID_RENTAL_STATUS);
        }

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        order.setStatus(RentalStatus.REJECTED);
        order.setStaff(staff);
        order.setRejectionReason(request.getReason());
        
        return rentalOrderMapper.toResponse(rentalOrderRepository.save(order));
    }

    @Transactional
    public RentalOrderResponse prepareHandoverOrder(Long orderId, String staffId) {
        RentalOrder order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RENTAL_ORDER_NOT_FOUND));

        if (order.getStatus() != RentalStatus.CONFIRMED) {
            throw new AppException(ErrorCode.INVALID_RENTAL_STATUS);
        }

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        order.setStatus(RentalStatus.READY_FOR_HANDOVER);
        order.setStaff(staff);
        
        return rentalOrderMapper.toResponse(rentalOrderRepository.save(order));
    }

    @Transactional
    public RentalOrderResponse handOverOrder(Long orderId, String staffId) {
        RentalOrder order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RENTAL_ORDER_NOT_FOUND));

        if (order.getStatus() != RentalStatus.READY_FOR_HANDOVER && order.getStatus() != RentalStatus.CONFIRMED) {
            throw new AppException(ErrorCode.INVALID_RENTAL_STATUS);
        }

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        order.setStatus(RentalStatus.HANDED_OVER);
        order.setStaff(staff);
        
        return rentalOrderMapper.toResponse(rentalOrderRepository.save(order));
    }

    @Transactional
    public RentalOrderResponse completeOrder(Long orderId, String staffId) {
        RentalOrder order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RENTAL_ORDER_NOT_FOUND));

        if (order.getStatus() != RentalStatus.HANDED_OVER) {
            throw new AppException(ErrorCode.INVALID_RENTAL_STATUS);
        }

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        order.setStatus(RentalStatus.COMPLETED);
        order.setStaff(staff);
        
        return rentalOrderMapper.toResponse(rentalOrderRepository.save(order));
    }

    @Transactional
    public RentalOrderResponse cancelOrder(Long orderId) {
        RentalOrder order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RENTAL_ORDER_NOT_FOUND));

        if (order.getStatus() != RentalStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể hủy đơn khi đang chờ xác nhận");
        }

        order.setStatus(RentalStatus.CANCELLED);
        
        return rentalOrderMapper.toResponse(rentalOrderRepository.save(order));
    }

    @Transactional
    public RentalOrderResponse rejectOrder(Long orderId, String staffId, String reason) {
        RentalOrder order = rentalOrderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.RENTAL_ORDER_NOT_FOUND));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        order.setStatus(RentalStatus.REJECTED);
        order.setRejectionReason(reason);
        order.setStaff(staff);
        
        return rentalOrderMapper.toResponse(rentalOrderRepository.save(order));
    }
}
