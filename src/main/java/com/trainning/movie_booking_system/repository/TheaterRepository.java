package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TheaterRepository extends JpaRepository<Theater,Long> {

    /**
     * Check if a theater exists by name
     * @param name theater name
     * @return true if exists, false otherwise
     */
    boolean existsByName(String name);

}
