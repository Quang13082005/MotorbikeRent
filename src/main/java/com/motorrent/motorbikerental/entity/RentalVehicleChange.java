package com.motorrent.motorbikerental.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name="rental_vehicle_changes") @Data @NoArgsConstructor
public class RentalVehicleChange {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id",nullable=false) private RentalOrder rentalOrder;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="old_vehicle_id",nullable=false) private Motorbike oldVehicle;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="new_vehicle_id",nullable=false) private Motorbike newVehicle;
 private String type="BEFORE_DELIVERY";
 private String status="WAITING_FOR_PAYMENT";
 private String oldVehicleName,newVehicleName,oldPlate,newPlate;
 private Integer oldDailyRate,newDailyRate,oldRentalFee,newRentalFee,oldDeliveryFee,newDeliveryFee;
 private Integer oldTotal,newTotal,oldSecurityDeposit,newSecurityDeposit,newDepositDue;
 private Integer paidBefore,balancePaidBefore,additionalPayment,creditAmount;
 private Integer newPickupFee,newReturnFee;
 private String newPickupType,newPickupAddress,newReturnType,newReturnAddress;
 private Double newPickupLat,newPickupLng,newReturnLat,newReturnLng;
 private LocalDateTime plannedPickup,plannedReturn,expiresAt,createdAt,processedAt;
 @Column(length=1000) private String reason;
 private String processedByStaffId,cancelledByStaffId,refundedByStaffId;
 private Integer refundedAmount;
 private LocalDateTime refundedAt;
 private LocalDateTime extensionStartTime,newPlannedReturnTime,actualVehicleChangeTime,financialApprovedAt;
 private Integer extraRentalAmount,vehicleChangeFee,additionalDepositAmount;
 private String changedByStaffId;
 @Column(length=500) private String exchangeAddress;
}
