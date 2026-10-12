package com.motorrent.motorbikerental.entity;
import com.motorrent.motorbikerental.enums.RentalStatus;
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
@Table(name = "rental_orders")
public class RentalOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    Long id;

    @Column(unique = true, nullable = false)
    String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorbike_id", nullable = false)
    Motorbike motorbike;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    User staff;

    @Column(nullable = false)
    LocalDateTime pickupDate;

    @Column(nullable = false)
    LocalDateTime returnDate;

    LocalDateTime actualReturnDate;

    @Column(nullable = false)
    Integer days;

    @Builder.Default
    String pickupType = "STORE";

    String pickupAddress;

    @Column(nullable = false)
    Integer totalPrice;
    Integer rentalFee;
    // Historical snapshot only; promotions are no longer offered.
    String promotionCode;
    Integer discountAmount;
    Integer deliveryFee;
    Integer pickupFee;
    Integer returnFee;
    Double pickupLat;
    Double pickupLng;
    Double returnLat;
    Double returnLng;
    Integer securityDeposit;
    Integer depositDue;
    Integer dailyRate;
    Integer balancePaid;
    String returnType;
    String returnAddress;
    LocalDateTime paymentExpiresAt;

    @Builder.Default
    Integer depositPaid = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    RentalStatus status = RentalStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    String note;
    @Column(columnDefinition="TEXT") String identitySnapshot;
    Boolean termsAccepted;

    @Column(columnDefinition = "TEXT")
    String rejectionReason;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public int getRemainingRentalAmount(){return Math.max(0,totalPrice-java.util.Objects.requireNonNullElse(depositPaid,0)-java.util.Objects.requireNonNullElse(balancePaid,0));}
    public int getRentalCreditAmount(){return Math.max(0,java.util.Objects.requireNonNullElse(depositPaid,0)+java.util.Objects.requireNonNullElse(balancePaid,0)-totalPrice);}

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
