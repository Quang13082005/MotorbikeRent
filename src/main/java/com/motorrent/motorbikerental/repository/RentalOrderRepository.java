package com.motorrent.motorbikerental.repository;
import com.motorrent.motorbikerental.entity.RentalOrder;
import com.motorrent.motorbikerental.enums.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RentalOrderRepository extends JpaRepository<RentalOrder, Long> {
    
    List<RentalOrder> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    
    List<RentalOrder> findByStatusOrderByCreatedAtDesc(RentalStatus status);
    
    // Check overlapping dates
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM RentalOrder r " +
           "WHERE r.motorbike.id = :motorbikeId " +
           "AND r.status NOT IN ('REJECTED', 'CANCELLED', 'COMPLETED') " +
           "AND r.pickupDate < :returnDate " +
           "AND r.returnDate > :pickupDate")
    boolean existsOverlappingOrder(
            @Param("motorbikeId") Long motorbikeId, 
            @Param("pickupDate") LocalDateTime pickupDate, 
            @Param("returnDate") LocalDateTime returnDate);
}