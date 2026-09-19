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

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
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

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword(), request.getConfirmPassword());
        return ApiResponse.success();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return ApiResponse.success(authService.getUserFromToken(authHeader.substring(7)));
        }
        return ApiResponse.success("Token invalid", null);
    }
}