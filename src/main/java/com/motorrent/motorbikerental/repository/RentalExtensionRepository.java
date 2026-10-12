package com.motorrent.motorbikerental.repository;
import com.motorrent.motorbikerental.entity.RentalExtension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RentalExtensionRepository extends JpaRepository<RentalExtension, Long> {
 @org.springframework.data.jpa.repository.Query("select e.rentalOrder.id from RentalExtension e where e.id=:id")
 java.util.Optional<Long> orderIdFor(@org.springframework.data.repository.query.Param("id") Long id);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select e from RentalExtension e where e.id=:id")
 java.util.Optional<RentalExtension> lockById(@org.springframework.data.repository.query.Param("id") Long id);
 @org.springframework.data.jpa.repository.Query("select e from RentalExtension e where e.rentalOrder.id=:id order by e.createdAt desc,e.id desc")
 java.util.List<RentalExtension> findByRentalOrderIdOrderByCreatedAtDesc(@org.springframework.data.repository.query.Param("id") Long id);
 boolean existsByRentalOrderIdAndStatus(Long id,String status);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select e from RentalExtension e where e.rentalOrder.id=:id and e.status in ('PENDING','WAITING_FOR_PAYMENT') order by e.id")
 java.util.List<RentalExtension> lockOpen(@org.springframework.data.repository.query.Param("id") Long id);
 String HOLD = " from RentalExtension e where e.motorbikeId=:bike and e.rentalOrder.id<>:excluded and e.status='WAITING_FOR_PAYMENT' and e.expiresAt>:now and e.rentalOrder.status in ('HANDED_OVER','EXTENDED','OVERDUE') and e.rentalOrder.motorbike.id=e.motorbikeId and e.rentalOrder.returnDate=e.originalReturnDate and e.originalReturnDate<:end and e.newReturnDate>:start";
 @org.springframework.data.jpa.repository.Query("select count(e)"+HOLD)
 long held(@org.springframework.data.repository.query.Param("bike") Long bike,@org.springframework.data.repository.query.Param("excluded") Long excluded,@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,@org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end,@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select e"+HOLD+" order by e.id")
 java.util.List<RentalExtension> lockHeld(@org.springframework.data.repository.query.Param("bike") Long bike,@org.springframework.data.repository.query.Param("excluded") Long excluded,@org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,@org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end,@org.springframework.data.repository.query.Param("now") java.time.LocalDateTime now);
}
