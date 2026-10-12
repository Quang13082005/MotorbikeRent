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
    private final com.motorrent.motorbikerental.repository.MotorbikeRepository motorbikes;
    private final RentalOrderRepository rentalOrderRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createReview(ReviewRequest request, String customerId) {
        motorbikes.lockById(rentalOrderRepository.motorbikeIdFor(request.getOrderId()).orElseThrow()).orElseThrow();
        RentalOrder order = rentalOrderRepository.lockById(request.getOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.RENTAL_ORDER_NOT_FOUND));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ được đánh giá đơn thuê của bạn");
        }

        if (order.getStatus() != RentalStatus.COMPLETED) {
            throw new IllegalArgumentException("Chỉ được đánh giá đơn đã hoàn tất");
        }

        if (reviewRepository.existsByRentalOrderId(order.getId())) {
            throw new IllegalArgumentException("Đơn thuê đã được đánh giá");
        }

        User customer = userRepository.findById(customerId).orElseThrow();

        Review review = Review.builder()
                .rentalOrder(order)
                .customer(customer)
                .motorbike(order.getMotorbike())
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        if(request.getRating()==null || !Double.isFinite(request.getRating()) || request.getRating()<1 || request.getRating()>5 || request.getRating()%1!=0) throw new IllegalArgumentException("Chọn số sao từ 1 đến 5");
        if(request.getComment()!=null && request.getComment().length()>2000) throw new IllegalArgumentException("Đánh giá tối đa 2000 ký tự");
        reviewRepository.saveAndFlush(review);
        var bike=order.getMotorbike();var list=reviewRepository.findByMotorbikeId(bike.getId());bike.setReviewCount(list.size());bike.setRating(list.stream().mapToDouble(Review::getRating).average().orElse(0));
    }
}
