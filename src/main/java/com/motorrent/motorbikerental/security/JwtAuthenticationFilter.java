package com.motorrent.motorbikerental.security;
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
