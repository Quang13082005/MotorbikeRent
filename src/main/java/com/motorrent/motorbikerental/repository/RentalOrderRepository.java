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
    @Query("select count(r) from RentalOrder r where r.motorbike.id=:bike and r.id<>:excluded " +
      "and r.status not in ('CANCELLED','REJECTED','COMPLETED') " +
      "and (r.status<>'PENDING_PAYMENT' or r.paymentExpiresAt is null or r.paymentExpiresAt>:now) " +
      "and ((r.pickupDate<:end and r.returnDate>:start) or " +
      "(r.status in ('HANDED_OVER','EXTENDED','OVERDUE','RETURN_REQUESTED','RETURNED') and r.actualReturnDate is null and r.returnDate<=:now))")
    long conflicts(@Param("bike") Long bike,@Param("excluded") Long excluded,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end,@Param("now") LocalDateTime now);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RentalOrder r where r.motorbike.id=:bike and r.id<>:excluded " +
      "and r.status not in ('CANCELLED','REJECTED','COMPLETED') " +
      "and (r.status<>'PENDING_PAYMENT' or r.paymentExpiresAt is null or r.paymentExpiresAt>:now) " +
      "and ((r.pickupDate<:end and r.returnDate>:start) or " +
      "(r.status in ('HANDED_OVER','EXTENDED','OVERDUE','RETURN_REQUESTED','RETURNED') and r.actualReturnDate is null and r.returnDate<=:now))")
    java.util.List<RentalOrder> lockedConflicts(@Param("bike") Long bike,@Param("excluded") Long excluded,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end,@Param("now") LocalDateTime now);
    @Query("select r.motorbike.id from RentalOrder r where r.id=:id")
    java.util.Optional<Long> motorbikeIdFor(@Param("id") Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RentalOrder r where r.id=:id")
    java.util.Optional<RentalOrder> lockById(@Param("id") Long id);
    @Query("select count(r) from RentalOrder r where r.motorbike.id=:bike and r.id<>:excluded and r.status not in ('CANCELLED','REJECTED','COMPLETED') and r.pickupDate<:end and r.returnDate>:start")
    long overlapping(@Param("bike") Long bike,@Param("excluded") Long excluded,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);

    
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
