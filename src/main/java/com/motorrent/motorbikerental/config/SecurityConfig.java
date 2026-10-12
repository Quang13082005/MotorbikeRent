package com.motorrent.motorbikerental.config;

import com.motorrent.motorbikerental.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Profile("!preview")
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final com.motorrent.motorbikerental.repository.TokenAuthenticationRepository tokens;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.sessionAuthenticationStrategy(new org.springframework.security.web.authentication.session.NullAuthenticatedSessionStrategy()).csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse()).csrfTokenRequestHandler(new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler()).ignoringRequestMatchers("/api/payment/payos-webhook"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/auth/**",
                                "/api/auth/login", "/api/auth/register", "/api/auth/forgot-password", "/api/auth/verify-otp", "/api/auth/reset-password",
                                "/api/motorbikes/**",
                                
                                "/api/payment/payos-webhook", "/api/shop",
                                "/customer/home",
                                "/customer/shop-information",
                                "/customer/travel-places", "/api/travel-places/search",
                                "/customer/motorbike-list",
                                "/customer/motorbike-detail/**",
                                
                                "/static/**",
                                "/css/**",
                                "/js/**",
                                "/images/**", "/uploads/avatars/**",
                                "/favicon.ico",
                                "/error/**"
                        ).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/operations/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers("/staff/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                // Each request authenticates its JWT. Do not treat asset/page requests as new logins.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .addLogoutHandler((request, response, authentication) -> {
                            String token = JwtAuthenticationFilter.token(request);
                            if (token != null) tokens.findByTokenValue(token).ifPresent(t -> { t.setIsUsed(false); tokens.save(t); });
                        })
                        .deleteCookies("access_token")
                        .logoutSuccessUrl("/auth/login?logout")
                        .permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            String message = exception instanceof org.springframework.security.web.csrf.CsrfException
                                    ? "Phiên biểu mẫu đã thay đổi. Vui lòng tải lại trang rồi thử lại."
                                    : "Bạn không có quyền thực hiện thao tác này.";
                            response.getWriter().write("{\"message\":\"" + message + "\"}");
                        })
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(401); response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"message\":\"Vui lòng đăng nhập lại\"}"); return;
                            }
                            String redirectUrl = request.getRequestURI();
                            if (request.getQueryString() != null) {
                                redirectUrl += "?" + request.getQueryString();
                            }
                            response.sendRedirect("/auth/login?redirect=" + java.net.URLEncoder.encode(redirectUrl, "UTF-8"));
                        })
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
