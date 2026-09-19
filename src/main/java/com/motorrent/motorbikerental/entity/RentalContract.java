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

    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}