package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Role;
import com.trainning.movie_booking_system.utils.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    Optional<Role> findByName(RoleType name);
    boolean existsByName(RoleType name);
}
