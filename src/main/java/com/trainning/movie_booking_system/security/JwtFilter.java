package com.trainning.movie_booking_system.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        log.info("========== DoFilter Running =========");

        try {
            // Lấy JWT token từ header Authorization
            String authHeader = request.getHeader("Authorization");
            String token = null;
            String username = null;

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7); // Bỏ "Bearer " prefix
                log.debug("Extracted token from Bearer header");
                
                try {
                    username = jwtProvider.extractUsername(token);
                    log.debug("Extracted username from token: {}", username);
                } catch (Exception e) {
                    log.warn("[JWT] Failed to extract username from token: {}", e.getMessage(), e);
                    // Tiếp tục filter chain mà không set authentication
                    filterChain.doFilter(request, response);
                    return;
                }
            } else if (authHeader != null) {
                log.warn("Invalid Authorization header format. Expected 'Bearer <token>' but got: {}", authHeader);
            }

            // Nếu có username và chưa có authentication trong SecurityContext
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Load user details từ database
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                
                // Validate token
                if (jwtProvider.validateToken(token)) {
                    // Tạo authentication token
                    UsernamePasswordAuthenticationToken authToken = 
                        new UsernamePasswordAuthenticationToken(
                            userDetails, 
                            null, 
                            userDetails.getAuthorities()
                        );
                    
                    // Set details
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Set vào SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.info("JWT authentication successful for user: {}", username);
                }
            }
            
        } catch (Exception e) {
            log.error("[JWT] Cannot set user authentication: {}", e.getMessage(), e);
        }

        // Tiếp tục filter chain
        filterChain.doFilter(request, response);
    }
}
