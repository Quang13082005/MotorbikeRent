package com.motorrent.motorbikerental.entity;
import com.motorrent.motorbikerental.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.GenericGenerator;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "user_id", length = 36, nullable = false)
    String id;

    @Enumerated(EnumType.STRING)
    UserRole role;

    @Column(nullable = false, unique = true)
    String username;

    String firstName;
    String lastName;
    
    @Column(nullable = false, unique = true)
    String email;
    
    @Column(nullable = false)
    String password;
    
    String phone;
    Boolean gender;
    String address;
    String avatar;
    String idCard;
    String driverLicense;
    
    @Builder.Default
    String status = "ACTIVE";
    
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