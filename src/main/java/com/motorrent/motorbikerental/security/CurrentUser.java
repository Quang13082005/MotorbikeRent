package com.motorrent.motorbikerental.security;

import com.motorrent.motorbikerental.entity.*;
import com.motorrent.motorbikerental.enums.UserRole;
import com.motorrent.motorbikerental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
@RequiredArgsConstructor
public class CurrentUser {
    private final UserRepository users;
    public User get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập");
        User user = auth.getPrincipal() instanceof CustomUserDetails principal
                ? users.findById(principal.getUserId()).orElse(null)
                : users.findByUsername(auth.getName()).orElse(null);
        if (user == null || !"ACTIVE".equals(user.getStatus())) throw new AccessDeniedException("Tài khoản không khả dụng");
        return user;
    }
    public String id() { return get().getId(); }
    public User customer() {
        User user = get();
        if (user.getRole() != UserRole.CUSTOMER) throw new AccessDeniedException("Chỉ khách hàng được đặt thuê");
        return user;
    }
    public User operator() {
        User user = get();
        if (user.getRole() != UserRole.STAFF && user.getRole() != UserRole.ADMIN)
            throw new AccessDeniedException("Bạn không có quyền xử lý đơn");
        return user;
    }
    public void canRead(RentalOrder order) {
        User user = get();
        if (user.getRole() == UserRole.CUSTOMER && !user.getId().equals(order.getCustomer().getId()))
            throw new AccessDeniedException("Bạn không có quyền truy cập đơn này");
    }
    public void owns(RentalOrder order) {
        if (!customer().getId().equals(order.getCustomer().getId()))
            throw new AccessDeniedException("Bạn không có quyền truy cập đơn này");
    }
}
