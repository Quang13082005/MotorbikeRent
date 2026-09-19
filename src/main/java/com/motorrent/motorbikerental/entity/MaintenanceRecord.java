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
@Table(name = "maintenance_records")
public class MaintenanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorbike_id", nullable = false)
    Motorbike motorbike;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by", nullable = false)
    User reportedBy;

    String type;
    
    @Column(columnDefinition = "TEXT")
    String description;

    @Builder.Default
    Integer cost = 0;

    @Builder.Default
    String status = "IN_PROGRESS";

    LocalDateTime startedAt;
    LocalDateTime completedAt;
    LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}