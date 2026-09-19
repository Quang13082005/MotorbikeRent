package com.motorrent.motorbikerental.controller.api;

import com.motorrent.motorbikerental.dto.request.RejectRentalOrderRequest;
import com.motorrent.motorbikerental.dto.request.RentalOrderRequest;
import com.motorrent.motorbikerental.dto.response.RentalDetailResponse;
import com.motorrent.motorbikerental.dto.response.RentalOrderResponse;
import com.motorrent.motorbikerental.service.RentalOrderService;
import com.motorrent.motorbikerental.repository.UserRepository;
import com.motorrent.motorbikerental.entity.User;
import com.motorrent.motorbikerental.enums.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rental-orders")
@RequiredArgsConstructor
public class RentalOrderApiController {

    private final RentalOrderService rentalOrderService;
    private final UserRepository userRepository;

    // CUSTOMER ENDPOINTS
    @PostMapping
    public ResponseEntity<RentalOrderResponse> createOrder(@Valid @RequestBody RentalOrderRequest request, Authentication authentication) {
        // Temp workaround for user ID, assume ID 2 is customer for now, or extract from Auth later in Phase 2
        User customer = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.CUSTOMER)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No customer found"));
        String customerId = customer.getId(); 
        return ResponseEntity.ok(rentalOrderService.createOrder(request, customerId));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<List<RentalOrderResponse>> getMyOrders(Authentication authentication) {
        User customer = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.CUSTOMER)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No customer found"));
        String customerId = customer.getId(); 
        return ResponseEntity.ok(rentalOrderService.getCustomerOrders(customerId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RentalDetailResponse> getOrderDetail(@PathVariable Long id) {
        return ResponseEntity.ok(rentalOrderService.getOrderDetail(id));
    }

    // STAFF ENDPOINTS
    @GetMapping("/pending")
    public ResponseEntity<List<RentalOrderResponse>> getPendingOrders() {
        return ResponseEntity.ok(rentalOrderService.getPendingOrders());
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<RentalOrderResponse> confirmOrder(@PathVariable Long id, Authentication authentication) {
        User staff = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.STAFF)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No staff found"));
        String staffId = staff.getId();
        return ResponseEntity.ok(rentalOrderService.confirmOrder(id, staffId));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<RentalOrderResponse> rejectOrder(
            @PathVariable Long id, 
            @RequestParam String reason, 
            Authentication authentication) {
        User staff = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.STAFF)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No staff found"));
        String staffId = staff.getId();
        return ResponseEntity.ok(rentalOrderService.rejectOrder(id, staffId, reason));
    }

    @PostMapping("/{id}/prepare-handover")
    public ResponseEntity<RentalOrderResponse> prepareHandoverOrder(@PathVariable Long id, Authentication authentication) {
        User staff = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.STAFF)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No staff found"));
        String staffId = staff.getId();
        return ResponseEntity.ok(rentalOrderService.prepareHandoverOrder(id, staffId));
    }

    @PostMapping("/{id}/hand-over")
    public ResponseEntity<RentalOrderResponse> handOverOrder(@PathVariable Long id, Authentication authentication) {
        User staff = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.STAFF)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No staff found"));
        String staffId = staff.getId();
        return ResponseEntity.ok(rentalOrderService.handOverOrder(id, staffId));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<RentalOrderResponse> completeOrder(@PathVariable Long id, Authentication authentication) {
        User staff = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.STAFF)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No staff found"));
        String staffId = staff.getId();
        return ResponseEntity.ok(rentalOrderService.completeOrder(id, staffId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<RentalOrderResponse> cancelOrder(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(rentalOrderService.cancelOrder(id));
    }
}