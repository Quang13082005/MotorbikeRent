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
@Table(name = "motorbike_brands")
public class MotorbikeBrand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "brand_id")
    Long id;

    @Column(nullable = false)
    String name;

    String logoUrl;
    String country;

    @Builder.Default
    String status = "ACTIVE";
}