package com.motorrent.motorbikerental.service;

import com.motorrent.motorbikerental.dto.request.ReviewRequest;
import com.motorrent.motorbikerental.entity.RentalOrder;
import com.motorrent.motorbikerental.entity.Review;
import com.motorrent.motorbikerental.entity.User;
import com.motorrent.motorbikerental.enums.RentalStatus;
import com.motorrent.motorbikerental.exception.AppException;
import com.motorrent.motorbikerental.exception.ErrorCode;
import com.motorrent.motorbikerental.repository.RentalOrderRepository;
import com.motorrent.motorbikerental.repository.ReviewRepository;
import com.motorrent.motorbikerental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final RentalOrderRepository rentalOrderRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createReview(ReviewRequest request, String customerId) {
        RentalOrder order = rentalOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.RENTAL_ORDER_NOT_FOUND));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new RuntimeException("Unauthorized: You can only review your own orders");
        }

        if (order.getStatus() != RentalStatus.COMPLETED) {
            throw new RuntimeException("Can only review COMPLETED orders");
        }

        if (reviewRepository.existsByRentalOrderId(order.getId())) {
            throw new RuntimeException("Review already exists for this order");
        }

        User customer = userRepository.findById(customerId).orElseThrow();

        Review review = Review.builder()
                .rentalOrder(order)
                .customer(customer)
                .motorbike(order.getMotorbike())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        reviewRepository.save(review);
    }
}
