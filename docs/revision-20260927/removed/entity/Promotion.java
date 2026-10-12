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
@Table(name = "promotions")
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promotion_id")
    Long id;

    @Column(unique = true, nullable = false)
    String code;

    String name;
    
    @Column(columnDefinition = "TEXT")
    String description;

    @Builder.Default
    String discountType = "PERCENTAGE";

    @Column(nullable = false)
    Integer discountValue;

    @Builder.Default
    Integer minOrderAmount = 0;

    Integer maxDiscount;
    LocalDateTime startDate;
    LocalDateTime endDate;
    Integer usageLimit;

    @Builder.Default
    Integer usedCount = 0;

    @Builder.Default
    String status = "ACTIVE";

    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}