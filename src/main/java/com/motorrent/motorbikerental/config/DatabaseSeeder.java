package com.motorrent.motorbikerental.config;

import com.motorrent.motorbikerental.entity.User;
import com.motorrent.motorbikerental.enums.UserRole;
import com.motorrent.motorbikerental.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;

@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
@Configuration
@RequiredArgsConstructor
public class DatabaseSeeder {

    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData(UserRepository userRepository,
                                      com.motorrent.motorbikerental.repository.MotorbikeRepository motorbikeRepository,
                                      com.motorrent.motorbikerental.repository.MotorbikeBrandRepository brandRepository,
                                      com.motorrent.motorbikerental.repository.MotorbikeCategoryRepository categoryRepository) {
        return args -> {
            if (!userRepository.existsByUsername("admin_demo")) {
                User admin = User.builder()
                        .username("admin_demo")
                        .password(passwordEncoder.encode("admin123"))
                        .email("admin_demo@motorent.vn")
                        .firstName("Admin")
                        .lastName("Root")
                        .role(UserRole.ADMIN)
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now())
                        .build();
                userRepository.save(admin);
            }
            
            if (!userRepository.existsByUsername("staff_demo")) {
                User staff = User.builder()
                        .username("staff_demo")
                        .password(passwordEncoder.encode("staff123"))
                        .email("staff_demo@motorent.vn")
                        .firstName("Staff")
                        .lastName("Demo")
                        .role(UserRole.STAFF)
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now())
                        .build();
                userRepository.save(staff);
            }
            
            if (!userRepository.existsByUsername("customer_demo")) {
                User customer = User.builder()
                        .username("customer_demo")
                        .password(passwordEncoder.encode("customer123"))
                        .email("customer_demo@motorent.vn")
                        .firstName("Customer")
                        .lastName("Demo")
                        .role(UserRole.CUSTOMER)
                        .status("ACTIVE")
                        .createdAt(LocalDateTime.now())
                        .build();
                userRepository.save(customer);
            }

            if (brandRepository.count() == 0) {
                brandRepository.save(com.motorrent.motorbikerental.entity.MotorbikeBrand.builder().name("Honda").country("Japan").status("ACTIVE").build());
                brandRepository.save(com.motorrent.motorbikerental.entity.MotorbikeBrand.builder().name("Yamaha").country("Japan").status("ACTIVE").build());
            }

            if (categoryRepository.count() == 0) {
                categoryRepository.save(com.motorrent.motorbikerental.entity.MotorbikeCategory.builder().name("xe-ga").description("Xe tay ga tiện lợi").status("ACTIVE").build());
                categoryRepository.save(com.motorrent.motorbikerental.entity.MotorbikeCategory.builder().name("xe-so").description("Xe số tiết kiệm").status("ACTIVE").build());
                categoryRepository.save(com.motorrent.motorbikerental.entity.MotorbikeCategory.builder().name("xe-con-tay").description("Xe côn tay thể thao").status("ACTIVE").build());
            }

            if (motorbikeRepository.count() == 0) {
                com.motorrent.motorbikerental.entity.MotorbikeBrand honda = brandRepository.findAll().stream().filter(b -> b.getName().equals("Honda")).findFirst().orElse(null);
                com.motorrent.motorbikerental.entity.MotorbikeBrand yamaha = brandRepository.findAll().stream().filter(b -> b.getName().equals("Yamaha")).findFirst().orElse(null);
                
                com.motorrent.motorbikerental.entity.MotorbikeCategory xeGa = categoryRepository.findAll().stream().filter(c -> c.getName().equals("xe-ga")).findFirst().orElse(null);
                com.motorrent.motorbikerental.entity.MotorbikeCategory xeSo = categoryRepository.findAll().stream().filter(c -> c.getName().equals("xe-so")).findFirst().orElse(null);
                com.motorrent.motorbikerental.entity.MotorbikeCategory xeCon = categoryRepository.findAll().stream().filter(c -> c.getName().equals("xe-con-tay")).findFirst().orElse(null);

                java.util.List<com.motorrent.motorbikerental.entity.Motorbike> motorbikes = java.util.Arrays.asList(
                    com.motorrent.motorbikerental.entity.Motorbike.builder().name("Honda Air Blade 125").brand(honda).category(xeGa).year("2023").cc(125).transmission("Tự động").pricePerDay(150000).status(com.motorrent.motorbikerental.enums.MotorbikeStatus.AVAILABLE).build(),
                    com.motorrent.motorbikerental.entity.Motorbike.builder().name("Honda Vision").brand(honda).category(xeGa).year("2022").cc(110).transmission("Tự động").pricePerDay(120000).status(com.motorrent.motorbikerental.enums.MotorbikeStatus.AVAILABLE).build(),
                    com.motorrent.motorbikerental.entity.Motorbike.builder().name("Honda Lead").brand(honda).category(xeGa).year("2023").cc(125).transmission("Tự động").pricePerDay(140000).status(com.motorrent.motorbikerental.enums.MotorbikeStatus.AVAILABLE).build(),
                    com.motorrent.motorbikerental.entity.Motorbike.builder().name("Yamaha Grande").brand(yamaha).category(xeGa).year("2024").cc(125).transmission("Tự động").pricePerDay(150000).status(com.motorrent.motorbikerental.enums.MotorbikeStatus.RENTED).build(),
                    com.motorrent.motorbikerental.entity.Motorbike.builder().name("Yamaha NVX 155").brand(yamaha).category(xeGa).year("2023").cc(155).transmission("Tự động").pricePerDay(180000).status(com.motorrent.motorbikerental.enums.MotorbikeStatus.AVAILABLE).build(),
                    com.motorrent.motorbikerental.entity.Motorbike.builder().name("Honda Wave Alpha").brand(honda).category(xeSo).year("2023").cc(110).transmission("Số tròn").pricePerDay(100000).status(com.motorrent.motorbikerental.enums.MotorbikeStatus.AVAILABLE).build(),
                    com.motorrent.motorbikerental.entity.Motorbike.builder().name("Honda Wave RSX").brand(honda).category(xeSo).year("2022").cc(110).transmission("Số tròn").pricePerDay(110000).status(com.motorrent.motorbikerental.enums.MotorbikeStatus.RENTED).build(),
                    com.motorrent.motorbikerental.entity.Motorbike.builder().name("Honda Future 125").brand(honda).category(xeSo).year("2024").cc(125).transmission("Số tròn").pricePerDay(130000).status(com.motorrent.motorbikerental.enums.MotorbikeStatus.AVAILABLE).build(),
                    com.motorrent.motorbikerental.entity.Motorbike.builder().name("Yamaha Sirius").brand(yamaha).category(xeSo).year("2022").cc(110).transmission("Số tròn").pricePerDay(100000).status(com.motorrent.motorbikerental.enums.MotorbikeStatus.AVAILABLE).build(),
                    com.motorrent.motorbikerental.entity.Motorbike.builder().name("Yamaha Jupiter").brand(yamaha).category(xeSo).year("2023").cc(115).transmission("Số tròn").pricePerDay(120000).status(com.motorrent.motorbikerental.enums.MotorbikeStatus.AVAILABLE).build(),
                    com.motorrent.motorbikerental.entity.Motorbike.builder().name("Honda Winner X").brand(honda).category(xeCon).year("2023").cc(150).transmission("Côn tay").pricePerDay(180000).status(com.motorrent.motorbikerental.enums.MotorbikeStatus.AVAILABLE).build(),
                    com.motorrent.motorbikerental.entity.Motorbike.builder().name("Yamaha Exciter 155").brand(yamaha).category(xeCon).year("2024").cc(155).transmission("Côn tay").pricePerDay(200000).status(com.motorrent.motorbikerental.enums.MotorbikeStatus.RENTED).build()
                );
                motorbikeRepository.saveAll(motorbikes);
            }
        };
    }
}
