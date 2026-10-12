package com.motorrent.motorbikerental.service;
import com.motorrent.motorbikerental.dto.request.LoginRequest;
import com.motorrent.motorbikerental.dto.request.RegisterRequest;
import com.motorrent.motorbikerental.dto.response.LoginResponse;
import com.motorrent.motorbikerental.dto.response.UserResponse;
import com.motorrent.motorbikerental.entity.TokenAuthentication;
import com.motorrent.motorbikerental.entity.User;
import com.motorrent.motorbikerental.enums.UserRole;
import com.motorrent.motorbikerental.exception.AppException;
import com.motorrent.motorbikerental.exception.ErrorCode;
import com.motorrent.motorbikerental.mapper.UserMapper;
import com.motorrent.motorbikerental.entity.OtpToken;
import com.motorrent.motorbikerental.repository.OtpTokenRepository;
import com.motorrent.motorbikerental.repository.TokenAuthenticationRepository;
import com.motorrent.motorbikerental.repository.UserRepository;
import com.motorrent.motorbikerental.security.JwtUtil;
import java.util.Random;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {
    UserRepository userRepository;
    TokenAuthenticationRepository tokenRepo;
    OtpTokenRepository otpTokenRepository;
    PasswordEncoder passwordEncoder;
    JwtUtil jwtUtil;
    UserMapper userMapper;
    EmailService emailService;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String identifier = request.getUsername();
        User user = userRepository.findByUsername(identifier)
                .orElseGet(() -> userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findByPhone(identifier)
                        .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS, "Tài khoản không tồn tại"))));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Sai mật khẩu");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new AppException(ErrorCode.USER_BLOCKED, "Tài khoản bị khóa");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        TokenAuthentication tokenEntity = TokenAuthentication.builder()
                .tokenValue(token)
                .isUsed(true)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .user(user)
                .build();
        tokenRepo.save(tokenEntity);

        return new LoginResponse("Đăng nhập thành công", token);
    }

    @Transactional
    public void register(RegisterRequest request) {
        if(request.getUsername()==null || !request.getUsername().matches("[a-zA-Z0-9_@.+-]{3,100}") || request.getEmail()==null || !request.getEmail().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw new IllegalArgumentException("Tên đăng nhập hoặc email không hợp lệ");
        validatePassword(request.getPassword());
        if(request.getFirstName()==null || request.getFirstName().isBlank() || request.getLastName()==null || request.getLastName().isBlank())throw new IllegalArgumentException("Vui lòng nhập đầy đủ họ và tên");
        if(request.getPhone()==null || !request.getPhone().matches("[+0-9 ()-]{9,20}"))throw new IllegalArgumentException("Số điện thoại không hợp lệ");
        if(userRepository.findByPhone(request.getPhone()).isPresent())throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED, "Username đã tồn tại");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_EXISTED, "Email đã tồn tại");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH, "Mật khẩu không khớp");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(UserRole.CUSTOMER)
                .status("ACTIVE")
                .build();

        userRepository.save(user);
        
        // Gửi email chào mừng
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName() + " " + user.getLastName());
        } catch (Exception e) {
            System.err.println("Không thể gửi email chào mừng: " + e.getMessage());
            // Không throw exception để quá trình đăng ký vẫn thành công
        }
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy tài khoản với email này"));

        // Tạo mã OTP 6 số ngẫu nhiên
        String otpCode = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        
        // Lưu OTP vào DB
        OtpToken otpToken = otpTokenRepository.findByEmail(email)
                .orElse(new OtpToken());
        otpToken.setEmail(email);
        otpToken.setOtpCode(otpCode);
        otpToken.setExpiryTime(LocalDateTime.now().plusMinutes(5)); // Hết hạn sau 5 phút
        
        otpTokenRepository.save(otpToken);

        // Gửi email
        try {
            emailService.sendOtpEmail(email, otpCode);
        } catch (Exception e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Lỗi gửi email OTP. Hệ thống chưa cấu hình SMTP.");
        }
    }

    public void verifyOtp(String email, String otpCode) {
        OtpToken otpToken = otpTokenRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN, "Không tìm thấy yêu cầu OTP cho email này"));
                
        if (otpToken.isExpired()) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED, "Mã OTP đã hết hạn");
        }
        
        if (!otpToken.getOtpCode().equals(otpCode)) {
            throw new AppException(ErrorCode.INVALID_TOKEN, "Mã OTP không chính xác");
        }
    }

    @Transactional
    public void resetPassword(String email, String otpCode, String newPassword, String confirmPassword) {
        validatePassword(newPassword);
        if (!newPassword.equals(confirmPassword)) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH, "Mật khẩu không khớp");
        }
        
        // Kiểm tra lại OTP lần nữa trước khi đổi mật khẩu
        verifyOtp(email, otpCode);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy user"));
                
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenRepo.revokeAll(user.getId());
        // Xóa OTP sau khi dùng thành công
        OtpToken otpToken = otpTokenRepository.findByEmail(email).get();
        otpTokenRepository.delete(otpToken);
    }

    @Transactional
    public void logout(String tokenValue) {
        TokenAuthentication token = tokenRepo.findByTokenValue(tokenValue).orElse(null);
        if (token != null) {
            token.setIsUsed(false);
            tokenRepo.save(token);
        }
    }

    public UserResponse getUserFromToken(String token) {
        String username = jwtUtil.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy user"));
        return userMapper.toResponse(user);
    }
    private void validatePassword(String password) {
        if(password==null || password.length()<8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw new IllegalArgumentException("Mật khẩu từ 8 ký tự và tối đa 72 byte");
    }
}