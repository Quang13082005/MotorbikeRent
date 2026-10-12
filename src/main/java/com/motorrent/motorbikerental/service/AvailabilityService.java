package com.motorrent.motorbikerental.service;

import com.motorrent.motorbikerental.entity.*;
import com.motorrent.motorbikerental.enums.*;
import com.motorrent.motorbikerental.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

/** Reads are advisory; mutations must lock the vehicle before calling requireAvailable. */
@Service @RequiredArgsConstructor
public class AvailabilityService {
 private final RentalOrderRepository orders;
 private final MotorbikeRepository bikes;
 private final RentalVehicleChangeRepository changes;
 private final RentalExtensionRepository extensions;
 @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
 public static LocalDateTime now(){return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));}
 @Transactional(readOnly=true)
 public boolean available(Long bikeId,Long excluded,LocalDateTime start,LocalDateTime end){
  var bike=bikes.findById(bikeId).orElseThrow();return available(bike,excluded,start,end);
 }
 public boolean available(Motorbike bike,Long excluded,LocalDateTime start,LocalDateTime end){
  PricingService.days(start,end);
  if(bike.getStatus()==MotorbikeStatus.MAINTENANCE||bike.getStatus()==MotorbikeStatus.UNAVAILABLE)return false;
  long skip=excluded==null?-1L:excluded;
  return orders.conflicts(bike.getId(),skip,start,end,now())==0 && changes.held(bike.getId(),skip,start,end,now())==0 && extensions.held(bike.getId(),skip,start,end,now())==0 && changes.heldDuring(bike.getId(),skip,start,end,now())==0;
 }
 public void requireAvailable(Motorbike bike,Long excluded,LocalDateTime start,LocalDateTime end){
  if(!availableLocked(bike,excluded,start,end))throw new IllegalArgumentException("Xe không còn trống trong thời gian đã chọn");
 }

 @Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
 public boolean availableLocked(Motorbike bike,Long excluded,LocalDateTime start,LocalDateTime end){
  PricingService.days(start,end);
  if(bike.getStatus()==MotorbikeStatus.MAINTENANCE||bike.getStatus()==MotorbikeStatus.UNAVAILABLE)return false;
  // A locking read sees commits made while waiting for the vehicle lock, also under MySQL REPEATABLE READ.
  long skip=excluded==null?-1L:excluded;
  return orders.lockedConflicts(bike.getId(),skip,start,end,now()).isEmpty() && changes.lockHeld(bike.getId(),skip,start,end,now()).isEmpty() && extensions.lockHeld(bike.getId(),skip,start,end,now()).isEmpty() && changes.lockHeldDuring(bike.getId(),skip,start,end,now()).isEmpty();
 }
 /** Acquire in stable vehicle -> order order; reject a stale vehicle association. */
 @Transactional(propagation=org.springframework.transaction.annotation.Propagation.MANDATORY)
 public RentalOrder lockOrder(Long id){
  Long vehicle=orders.motorbikeIdFor(id).orElseThrow();
  var bike=bikes.lockById(vehicle).orElseThrow();entityManager.refresh(bike,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
  var order=orders.lockById(id).orElseThrow();entityManager.refresh(order,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
  if(!vehicle.equals(order.getMotorbike().getId()))throw new IllegalArgumentException("Xe của đơn vừa thay đổi, vui lòng tải lại");
  return order;
 }
}
