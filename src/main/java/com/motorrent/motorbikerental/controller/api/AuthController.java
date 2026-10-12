package com.motorrent.motorbikerental.controller.api;
import com.motorrent.motorbikerental.dto.common.ApiResponse;
import com.motorrent.motorbikerental.dto.request.*;
import com.motorrent.motorbikerental.dto.response.LoginResponse;
import com.motorrent.motorbikerental.dto.response.UserResponse;
import com.motorrent.motorbikerental.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final com.motorrent.motorbikerental.security.CurrentUser currentUser;
    private final com.motorrent.motorbikerental.mapper.UserMapper userMapper;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest, jakarta.servlet.http.HttpServletResponse response) {
        var result = authService.login(request);
        // Rotate once at successful login; the next page supplies the new token.
        org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse()
                .saveToken(null, servletRequest, response);
        return ApiResponse.success(result);
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ApiResponse.success();
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ApiResponse.success();
    }
    
    @PostMapping("/verify-otp")
    public ApiResponse<Void> verifyOtp(@RequestBody VerifyOtpRequest request) {
        authService.verifyOtp(request.getEmail(), request.getOtpCode());
        return ApiResponse.success();
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getEmail(), request.getOtpCode(), request.getNewPassword(), request.getConfirmPassword());
        return ApiResponse.success();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        String token = com.motorrent.motorbikerental.security.JwtAuthenticationFilter.token(request);
        if(token != null) authService.logout(token);
        var cookie=new jakarta.servlet.http.Cookie("access_token", ""); cookie.setPath("/"); cookie.setMaxAge(0); response.addCookie(cookie);
        return ApiResponse.success();
    }
    @GetMapping("/me")
    public ApiResponse<UserResponse> me() { return ApiResponse.success(userMapper.toResponse(currentUser.get())); }
}
