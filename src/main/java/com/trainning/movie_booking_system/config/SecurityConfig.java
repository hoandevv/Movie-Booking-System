package com.trainning.movie_booking_system.config;

import com.trainning.movie_booking_system.entity.Account;
import com.trainning.movie_booking_system.entity.AccountHasRole;
import com.trainning.movie_booking_system.entity.Role;
import com.trainning.movie_booking_system.repository.AccountRepository;
import com.trainning.movie_booking_system.repository.RoleRepository;
import com.trainning.movie_booking_system.security.CustomUserDetailsService;
import com.trainning.movie_booking_system.security.JwtFilter;
import com.trainning.movie_booking_system.utils.enums.RoleType;
import com.trainning.movie_booking_system.utils.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtFilter jwtFilter;

    //  Public endpoints (không cần token)
    private static final String[] PUBLIC_ENDPOINTS = {
            "/",
            "/api/auth/**",
            "/api/v1/auth/**",   // thêm v1
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

    // Public GET endpoints (người dùng xem phim, suất chiếu...)
    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/api/movies/**",
            "/api/v1/movies/**",      // thêm v1
            "/api/theaters/**",
            "/api/v1/theaters/**",
            "/api/showtimes/**",
            "/api/v1/showtimes/**",
            "/api/seats/**",
            "/api/v1/seats/**"
    };

    // User endpoints (cần token)
    private static final String[] USER_ENDPOINTS = {
            "/api/bookings/**",
            "/api/v1/bookings/**",
            "/api/vouchers/validate",
            "/api/v1/vouchers/validate",
            "/api/vouchers/apply",
            "/api/v1/vouchers/apply"
    };

    // Admin endpoints
    private static final String[] ADMIN_ENDPOINTS = {
            "/api/admin/**",
            "/api/v1/admin/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) //  dùng cấu hình cors thật
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz
                        // Public hoàn toàn
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // GET public
                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()
                        // User authenticated
                        .requestMatchers(USER_ENDPOINTS).authenticated()
                        // Admin
                        .requestMatchers(ADMIN_ENDPOINTS).hasRole("ADMIN")
                        // Còn lại cần auth
                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
//    @Bean
//    CommandLineRunner createAdminAccount(AccountRepository accountRepo,
//                                         RoleRepository roleRepo,
//                                         PasswordEncoder encoder) {
//        return args -> {
//            //  Tạo role ADMIN nếu chưa có
//            if (!roleRepo.existsByName(RoleType.ADMIN)) {
//                roleRepo.save(Role.builder()
//                        .name(RoleType.ADMIN)
//                        .description(RoleType.ADMIN.getDescription())
//                        .build());
//            }
//
//            //  Nếu đã có account admin rồi thì bỏ qua
//            if (accountRepo.existsByUsername("admin")) {
//                return;
//            }
//
//            // 3️⃣ Tạo account admin
//            Account admin = Account.builder()
//                    .username("admin")
//                    .email("admin@moviebooking.com")
//                    .password(encoder.encode("admin123"))
//                    .status(UserStatus.ACTIVE)
//                    .emailVerified(true)
//                    .build();
//
//            //  Lấy role ADMIN
//            Role adminRole = roleRepo.findByName(RoleType.ADMIN)
//                    .orElseThrow(() -> new IllegalStateException("Role ADMIN not found"));
//
//            //  Tạo bản ghi AccountHasRole (join bảng)
//            AccountHasRole link = AccountHasRole.builder()
//                    .account(admin)
//                    .role(adminRole)
//                    .build();
//
//            admin.getAccountRoles().add(link);
//
//            //  Lưu account (cascade sẽ lưu luôn account_roles)
//            accountRepo.save(admin);
//        };
//    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}