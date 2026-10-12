package com.motorrent.motorbikerental.service;

import com.motorrent.motorbikerental.entity.Notification;
import com.motorrent.motorbikerental.entity.RentalOrder;
import com.motorrent.motorbikerental.repository.NotificationRepository;
import com.motorrent.motorbikerental.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;

@Service @RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notifications;
    private final CurrentUser current;
    public List<Notification> mine() { return notifications.findTop100ByUserIdOrderByCreatedAtDesc(current.id()); }
    @Transactional public void orderChanged(RentalOrder order) {
        send(order, "Cập nhật đơn " + order.getOrderCode(), "Trạng thái: " + new Display().status(order.getStatus()));
    }
    @Transactional public void send(RentalOrder order, String title, String message) {
        notifications.save(Notification.builder().user(order.getCustomer()).title(title).message(message)
                .type("RENTAL").link("/customer/rental-detail/" + order.getId()).build());
    }
    @Transactional public void read(Long id) {
        var notification = notifications.findById(id).orElseThrow(() -> new IllegalArgumentException("Thông báo không tồn tại"));
        if (!notification.getUser().getId().equals(current.id())) throw new AccessDeniedException("Thông báo không thuộc tài khoản của bạn");
        notification.setIsRead(true);
    }
}
