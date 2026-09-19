package com.motorrent.motorbikerental.entity;
import com.motorrent.motorbikerental.enums.MotorbikeStatus;
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
@Table(name = "motorbikes")
public class Motorbike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "motorbike_id")
    Long id;

    @Column(nullable = false)
    String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    MotorbikeBrand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    MotorbikeCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id")
    Station station;

    @Column(name = "year")
    String year;

    Integer cc;
    String transmission;
    String fuelConsumption;
    
    @Column(nullable = false)
    Integer pricePerDay;
    
    Integer pricePerWeek;
    
    @Builder.Default
    Integer depositAmount = 0;

    @Column(columnDefinition = "TEXT")
    String highlights;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(unique = true)
    String licensePlate;

    @Builder.Default
    Integer odoKm = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    MotorbikeStatus status = MotorbikeStatus.AVAILABLE;

    @Builder.Default
    Double rating = 0.0;

    @Builder.Default
    Integer reviewCount = 0;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

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