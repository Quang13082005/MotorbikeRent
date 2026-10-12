package com.motorrent.motorbikerental.entity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "rental_contracts")
public class RentalContract {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contract_id")
    Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    RentalOrder rentalOrder;

    @Column(unique = true)
    String contractCode;

    String customerSignatureUrl;
    String staffSignatureUrl;
    
    @Column(columnDefinition = "TEXT")
    String vehicleConditionBefore;
    
    @Column(columnDefinition = "TEXT")
    String vehicleConditionAfter;
    
    Integer odoBefore;
    Integer odoAfter;

    LocalDateTime handedOverAt;
    String handoverStaffId;
    LocalDateTime completedAt;
    String completionStaffId;

    LocalDateTime earlyReturnRequestedAt;
    @Column(columnDefinition = "TEXT")
    String earlyReturnReason;
    Integer earlyReturnRefundAmount;
    @Column(columnDefinition = "TEXT")
    String earlyReturnDecisionReason;
    String earlyReturnPaymentMethod;

    Integer assetDepositHeld;
    Integer assetDepositRefundAmount;
    Integer assetDepositDeductionAmount;
    @Column(columnDefinition = "TEXT")
    String depositDeductionReason;
    Integer lateFee;
    @Column(columnDefinition = "TEXT")
    String lateFeeReason;
    String lateFeePaymentMethod;

    public LocalDateTime getActualPickupTime() { return handedOverAt; }
    public void setActualPickupTime(LocalDateTime time) { this.handedOverAt = time; }
    public String getDeliveredByStaffId() { return handoverStaffId; }
    public void setDeliveredByStaffId(String id) { this.handoverStaffId = id; }
    public LocalDateTime getDeliveredAt() { return handedOverAt; }
    public void setDeliveredAt(LocalDateTime time) { this.handedOverAt = time; }

    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}