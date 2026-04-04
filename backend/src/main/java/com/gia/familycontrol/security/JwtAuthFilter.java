package com.gia.familycontrol.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String requestUri = request.getRequestURI();
        
        log.debug("Request to: {}", requestUri);
        
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            log.debug("Token found: {}...", token.substring(0, Math.min(20, token.length())));
            
            try {
                if (jwtUtil.validateToken(token)) {
                    String email = jwtUtil.extractEmail(token);
                    String role = jwtUtil.extractRole(token);
                    log.info("Authenticated: {} with role: {}", email, role);
                    
                    var auth = new UsernamePasswordAuthenticationToken(
                            email, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } else {
                    log.warn("Invalid token for request: {}", requestUri);
                }
            } catch (Exception e) {
                log.error("Token validation error: {}", e.getMessage());
            }
        } else {
            log.debug("No Authorization header for: {}", requestUri);
        }
        
        chain.doFilter(request, response);
    }
}
