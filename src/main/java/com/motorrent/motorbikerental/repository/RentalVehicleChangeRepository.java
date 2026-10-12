package com.motorrent.motorbikerental.repository;
import com.motorrent.motorbikerental.entity.RentalVehicleChange;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;

public interface RentalVehicleChangeRepository extends JpaRepository<RentalVehicleChange,Long> {
 @Query("select count(c) from RentalVehicleChange c where c.rentalOrder.id=:id and c.type='DURING_RENTAL' and (c.status='READY_FOR_EXCHANGE' or (c.status='WAITING_FOR_PAYMENT' and c.expiresAt>:now))")
 long openDuring(@Param("id") Long id,@Param("now") LocalDateTime now);
 @Query("select count(c) from RentalVehicleChange c where c.newVehicle.id=:bike and c.rentalOrder.id<>:excluded and c.type='DURING_RENTAL' and c.rentalOrder.status in ('HANDED_OVER','EXTENDED','OVERDUE','RETURN_REQUESTED','RETURNED') and (c.status='READY_FOR_EXCHANGE' or (c.status='WAITING_FOR_PAYMENT' and c.expiresAt>:now)) and c.extensionStartTime<:end and c.newPlannedReturnTime>:start")
 long heldDuring(@Param("bike") Long bike,@Param("excluded") Long excluded,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end,@Param("now") LocalDateTime now);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select c from RentalVehicleChange c where c.newVehicle.id=:bike and c.rentalOrder.id<>:excluded and c.type='DURING_RENTAL' and c.rentalOrder.status in ('HANDED_OVER','EXTENDED','OVERDUE','RETURN_REQUESTED','RETURNED') and (c.status='READY_FOR_EXCHANGE' or (c.status='WAITING_FOR_PAYMENT' and c.expiresAt>:now)) and c.extensionStartTime<:end and c.newPlannedReturnTime>:start order by c.id")
 List<RentalVehicleChange> lockHeldDuring(@Param("bike") Long bike,@Param("excluded") Long excluded,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end,@Param("now") LocalDateTime now);
 List<RentalVehicleChange> findByRentalOrderIdOrderByIdDesc(Long id);
 @Query("select c.rentalOrder.id from RentalVehicleChange c where c.id=:id") Optional<Long> orderId(@Param("id") Long id);
 @Query("select c.newVehicle.id from RentalVehicleChange c where c.id=:id") Optional<Long> vehicleId(@Param("id") Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from RentalVehicleChange c where c.id=:id") Optional<RentalVehicleChange> lockById(@Param("id") Long id);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select c from RentalVehicleChange c where c.rentalOrder.id=:id and c.status='WAITING_FOR_PAYMENT' order by c.id") List<RentalVehicleChange> lockPending(@Param("id") Long id);
 @Query("select count(c) from RentalVehicleChange c where c.newVehicle.id=:bike and c.rentalOrder.id<>:excluded and c.status='WAITING_FOR_PAYMENT' and c.rentalOrder.status in ('PENDING','CONFIRMED','READY_FOR_HANDOVER') and c.expiresAt>:now and c.plannedPickup<:end and c.plannedReturn>:start")
 long held(@Param("bike") Long bike,@Param("excluded") Long excluded,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end,@Param("now") LocalDateTime now);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select c from RentalVehicleChange c where c.newVehicle.id=:bike and c.rentalOrder.id<>:excluded and c.status='WAITING_FOR_PAYMENT' and c.rentalOrder.status in ('PENDING','CONFIRMED','READY_FOR_HANDOVER') and c.expiresAt>:now and c.plannedPickup<:end and c.plannedReturn>:start order by c.id")
 List<RentalVehicleChange> lockHeld(@Param("bike") Long bike,@Param("excluded") Long excluded,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end,@Param("now") LocalDateTime now);
}
