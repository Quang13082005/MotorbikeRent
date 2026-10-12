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

    @Column(name = "color")
    String color;

    @Column(name = "asset_value")
    Long assetValue;

    @OneToMany(mappedBy="motorbike")
    java.util.List<MotorbikeImage> images;

    public String getImagePrimary() {
        if(images==null || images.isEmpty()) return "/images/motorbike-placeholder.svg";
        String url=images.stream().filter(i->Boolean.TRUE.equals(i.getIsPrimary())).findFirst().orElse(images.get(0)).getImageUrl();
        if(url==null || url.isBlank()) return "/images/motorbike-placeholder.svg";
        return url.startsWith("http://") || url.startsWith("https://") || url.startsWith("/") ? url : "/"+url;
    }

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