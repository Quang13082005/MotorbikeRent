package com.motorrent.motorbikerental.repository;
import com.motorrent.motorbikerental.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByRentalOrderId(Long orderId);
 java.util.List<Review> findByMotorbikeId(Long id);
}