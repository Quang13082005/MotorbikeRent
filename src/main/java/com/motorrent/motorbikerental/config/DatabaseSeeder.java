package com.motorrent.motorbikerental.config;

import com.motorrent.motorbikerental.entity.User;
import com.motorrent.motorbikerental.enums.UserRole;
import com.motorrent.motorbikerental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
public class DatabaseSeeder {

    @Bean
    public CommandLineRunner initData(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                User admin = User.builder()
                        .username("admin_demo")
                        .password("admin123")
                        .email("admin@motorent.vn")
                        .firstName("Admin")
                        .lastName("Root")
                        .role(UserRole.ADMIN)
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now())
                        .build();
                
                User staff = User.builder()
                        .username("staff_demo")
                        .password("staff123")
                        .email("staff@motorent.vn")
                        .firstName("Staff")
                        .lastName("Demo")
                        .role(UserRole.STAFF)
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now())
                        .build();
                
                User customer = User.builder()
                        .username("customer_demo")
                        .password("customer123")
                        .email("customer@motorent.vn")
                        .firstName("Customer")
                        .lastName("Demo")
                        .role(UserRole.CUSTOMER)
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now())
                        .build();

                userRepository.save(admin);
                userRepository.save(staff);
                userRepository.save(customer);
            }
        };
    }
}
