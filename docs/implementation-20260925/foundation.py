from pathlib import Path
R=Path('src/main/java/com/motorrent/motorbikerental')
def put(p,s): (R/p).write_text(s,encoding='utf-8')
def edit(p,a,b):
 f=R/p; s=f.read_text('utf-8'); assert a in s,(p,a); f.write_text(s.replace(a,b),encoding='utf-8')
edit('config/DatabaseSeeder.java','@Configuration','@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")\n@Configuration')
edit('security/JwtUtil.java','.setSubject(username)','.setId(java.util.UUID.randomUUID().toString())\n                .setSubject(username)')
put('security/JwtAuthenticationFilter.java','''package com.motorrent.motorbikerental.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;
import com.motorrent.motorbikerental.repository.*;
@Component @RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
 private final JwtUtil jwtUtil;
 private final TokenAuthenticationRepository tokens;
 private final UserRepository users;
 public static String token(HttpServletRequest request) {
  String h=request.getHeader("Authorization");
  if(h!=null && h.startsWith("Bearer ")) return h.substring(7);
  if(request.getCookies()!=null) for(Cookie c:request.getCookies()) if("access_token".equals(c.getName())) return c.getValue();
  return null;
 }
 @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
  String token=token(request);
  if(token!=null) try {
   if(jwtUtil.validateToken(token)) {
    var saved=tokens.findByTokenValue(token).orElse(null);
    var user=users.findById(jwtUtil.extractUserId(token)).orElse(null);
    if(saved!=null && Boolean.TRUE.equals(saved.getIsUsed()) && saved.getExpiresAt()!=null && saved.getExpiresAt().isAfter(LocalDateTime.now()) && user!=null && "ACTIVE".equals(user.getStatus())) {
     var principal=new CustomUserDetails(user.getId(),user.getUsername(),user.getRole().name(),"");
     var auth=new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities());
     auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
     SecurityContextHolder.getContext().setAuthentication(auth);
    }
   }
  } catch(RuntimeException ignored) { SecurityContextHolder.clearContext(); }
  chain.doFilter(request,response);
 }
}
''')
edit('config/SecurityConfig.java','private final JwtAuthenticationFilter jwtAuthenticationFilter;','private final JwtAuthenticationFilter jwtAuthenticationFilter;\n    private final com.motorrent.motorbikerental.service.AuthService authService;')
edit('config/SecurityConfig.java','"/api/auth/**",','"/api/auth/login", "/api/auth/register", "/api/auth/forgot-password", "/api/auth/verify-otp", "/api/auth/reset-password",')
edit('config/SecurityConfig.java','"/api/ocr/**",','')
edit('config/SecurityConfig.java','"/api/payment/**",','"/api/payment/payos-webhook", "/api/shop",')
edit('config/SecurityConfig.java','"/customer/payment-result",','')
edit('config/SecurityConfig.java','.requestMatchers("/staff/**")','.requestMatchers("/api/admin/**").hasRole("ADMIN")\n                        .requestMatchers("/api/operations/**").hasAnyRole("STAFF", "ADMIN")\n                        .requestMatchers("/staff/**")')
edit('config/SecurityConfig.java','.logoutSuccessUrl("/auth/login?logout")','.addLogoutHandler((request, response, authentication) -> {\n                            String token = JwtAuthenticationFilter.token(request);\n                            if (token != null) authService.logout(token);\n                        })\n                        .deleteCookies("access_token")\n                        .logoutSuccessUrl("/auth/login?logout")')
edit('config/SecurityConfig.java','String redirectUrl = request.getRequestURI();','if (request.getRequestURI().startsWith("/api/")) {\n                                response.setStatus(401); response.setContentType("application/json;charset=UTF-8");\n                                response.getWriter().write("{\\"message\\":\\"Vui lòng đăng nhập lại\\"}"); return;\n                            }\n                            String redirectUrl = request.getRequestURI();')
edit('exception/GlobalExceptionHandler.java','public class GlobalExceptionHandler {','''public class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<?> denied(Exception e) { return ResponseEntity.status(403).body(java.util.Map.of("message", e.getMessage())); }
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<?> status(org.springframework.web.server.ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(java.util.Map.of("message", e.getReason() == null ? "Yêu cầu không hợp lệ" : e.getReason())); }
    @ExceptionHandler({IllegalArgumentException.class, org.springframework.dao.DataIntegrityViolationException.class})
    public ResponseEntity<?> invalid(Exception e) { return ResponseEntity.badRequest().body(java.util.Map.of("message", e instanceof IllegalArgumentException ? e.getMessage() : "Dữ liệu trùng hoặc đang được sử dụng")); }
''')
edit('exception/GlobalExceptionHandler.java','"Lỗi hệ thống: " + e.getMessage()','"Không thể xử lý yêu cầu. Vui lòng thử lại."')
edit('repository/TokenAuthenticationRepository.java','Optional<TokenAuthentication> findByTokenValue(String tokenValue);','''Optional<TokenAuthentication> findByTokenValue(String tokenValue);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update TokenAuthentication t set t.isUsed=false where t.user.id=:id")
    void revokeAll(@org.springframework.data.repository.query.Param("id") String id);''')
edit('controller/api/AuthController.java','private final AuthService authService;','private final AuthService authService;\n    private final com.motorrent.motorbikerental.security.CurrentUser currentUser;\n    private final com.motorrent.motorbikerental.mapper.UserMapper userMapper;')
start=(R/'controller/api/AuthController.java').read_text('utf-8').index('    @PostMapping("/logout")')
f=R/'controller/api/AuthController.java'; txt=f.read_text('utf-8'); f.write_text(txt[:start]+'''    @PostMapping("/logout")
    public ApiResponse<Void> logout(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        String token = com.motorrent.motorbikerental.security.JwtAuthenticationFilter.token(request);
        if(token != null) authService.logout(token);
        var cookie=new jakarta.servlet.http.Cookie("access_token", ""); cookie.setPath("/"); cookie.setMaxAge(0); response.addCookie(cookie);
        return ApiResponse.success();
    }
    @GetMapping("/me")
    public ApiResponse<UserResponse> me() { return ApiResponse.success(userMapper.toResponse(currentUser.get())); }
}
''',encoding='utf-8')
put('controller/api/RentalOrderApiController.java','''package com.motorrent.motorbikerental.controller.api;
import com.motorrent.motorbikerental.dto.request.*;
import com.motorrent.motorbikerental.dto.response.*;
import com.motorrent.motorbikerental.service.RentalOrderService;
import com.motorrent.motorbikerental.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/rental-orders") @RequiredArgsConstructor
public class RentalOrderApiController {
 private final RentalOrderService service; private final CurrentUser current;
 @PostMapping public RentalOrderResponse create(@Valid @RequestBody RentalOrderRequest r) { return service.createOrder(r,current.customer().getId()); }
 @PostMapping("/quote") public java.util.Map<String,Object> quote(@Valid @RequestBody RentalOrderRequest r) { current.customer(); return service.quote(r); }
 @GetMapping("/my-orders") public List<RentalOrderResponse> mine() { return service.getCustomerOrders(current.customer().getId()); }
 @GetMapping("/{id}") public RentalDetailResponse detail(@PathVariable Long id) { service.requireReadable(id); return service.getOrderDetail(id); }
 @GetMapping("/pending") public List<RentalOrderResponse> pending() { current.operator(); return service.getPendingOrders(); }
 @PostMapping("/{id}/confirm") public RentalOrderResponse confirm(@PathVariable Long id) { return service.confirmOrder(id,current.operator().getId()); }
 @PostMapping("/{id}/reject") public RentalOrderResponse reject(@PathVariable Long id,@RequestParam String reason) { return service.rejectOrder(id,current.operator().getId(),reason); }
 @PostMapping("/{id}/prepare-handover") public RentalOrderResponse prepare(@PathVariable Long id) { return service.prepareHandoverOrder(id,current.operator().getId()); }
 @PostMapping("/{id}/hand-over") public RentalOrderResponse handover(@PathVariable Long id) { return service.handOverOrder(id,current.operator().getId()); }
 @PostMapping("/{id}/complete") public RentalOrderResponse complete(@PathVariable Long id) { return service.completeOrder(id,current.operator().getId()); }
 @PostMapping("/{id}/cancel") public RentalOrderResponse cancel(@PathVariable Long id) { service.requireOwned(id); return service.cancelOrder(id); }
}
''')
f=R/'controller/api/ReviewApiController.java'; t=f.read_text('utf-8'); a=t.index('        // Temp workaround'); b=t.index('        reviewService.createReview',a); t=t[:a]+'        String customerId = currentUser.customer().getId();\n'+t[b:]; t=t.replace('private final UserRepository userRepository;','private final com.motorrent.motorbikerental.security.CurrentUser currentUser;'); f.write_text(t,encoding='utf-8')
