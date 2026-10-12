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
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    RentalOrder rentalOrder;

    @Column(nullable = false)
    Integer amount;

    @Builder.Default
    String paymentMethod = "CASH";

    @Column(unique=true) String transactionCode;
    @Column(unique=true) Long providerCode;
    String providerLinkId;
    @Column(length=2000) String checkoutUrl;
    String purpose;
    String transactionType;
    String direction;
    String processedByStaffId;
    LocalDateTime processedAt;
    @Column(length=2000) String note;
    @Column(columnDefinition="TEXT") String componentsJson;
    @Column(unique=true) String idempotencyKey;
    Long originalPaymentId;
    String sourceType;
    Long sourceId;
    Integer receivedAmount;
    LocalDateTime expiresAt;

    @Builder.Default
    String status = "PENDING";

    LocalDateTime paidAt;
    LocalDateTime createdAt;

    public String getStaffId() { return processedByStaffId; }
    public void setStaffId(String staffId) { this.processedByStaffId = staffId; }

    @PrePersist
    protected void onCreate() {
        com.motorrent.motorbikerental.service.Financials.normalize(this);
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { com.motorrent.motorbikerental.service.Financials.normalize(this); }
}
