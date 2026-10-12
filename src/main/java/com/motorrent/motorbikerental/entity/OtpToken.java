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
@Table(name = "otp_tokens")
public class OtpToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    
    @Column(nullable = false, unique = true)
    String email;
    
    @Column(nullable = false, length = 6)
    String otpCode;
    
    @Column(nullable = false)
    LocalDateTime expiryTime;
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryTime);
    }
}
