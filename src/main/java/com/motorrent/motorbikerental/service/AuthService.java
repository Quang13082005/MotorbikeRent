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
import com.motorrent.motorbikerental.repository.TokenAuthenticationRepository;
import com.motorrent.motorbikerental.repository.UserRepository;
import com.motorrent.motorbikerental.security.JwtUtil;
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
    PasswordEncoder passwordEncoder;
    JwtUtil jwtUtil;
    UserMapper userMapper;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS, "Tài khoản không tồn tại"));

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
    }

    public void forgotPassword(String email) {
        // Mock email sending
        System.out.println("Send reset password link to: " + email);
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH, "Mật khẩu không khớp");
        }
        TokenAuthentication tokenAuth = tokenRepo.findByTokenValue(token)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN, "Token không hợp lệ"));
        
        if (tokenAuth.getIsUsed() || tokenAuth.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED, "Token đã hết hạn hoặc được sử dụng");
        }

        User user = tokenAuth.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenAuth.setIsUsed(true);
        tokenRepo.save(tokenAuth);
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
}