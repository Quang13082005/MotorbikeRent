package com.motorrent.motorbikerental.entity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "motorbike_images")
public class MotorbikeImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorbike_id", nullable = false)
    Motorbike motorbike;

    @Column(nullable = false)
    String imageUrl;

    @Builder.Default
    Boolean isPrimary = false;

    @Builder.Default
    Integer sortOrder = 0;
}