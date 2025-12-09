package com.trainning.movie_booking_system.config;
import com.trainning.movie_booking_system.utils.enums.RoleType;
import com.trainning.movie_booking_system.entity.Role;
import com.trainning.movie_booking_system.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class DataInitializer {
    /**
     * Add Status emnum
     * */
    @Bean

    CommandLineRunner seedRoles(RoleRepository roleRepository) {
        return args -> {
            Arrays.stream(RoleType.values()).forEach(role -> {
                if (!roleRepository.existsByName(role)) {
                    roleRepository.save(Role.builder()
                            .name(role)
                            .description(role.getDescription())
                            .build());
                    System.out.println("✔ Inserted role: " + role);
                }
            });
        };
    }

}