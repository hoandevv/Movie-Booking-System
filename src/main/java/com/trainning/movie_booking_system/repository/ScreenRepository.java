package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Screen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ScreenRepository extends JpaRepository<Screen,Long> {

    /**
     * Check if a screen exists by its name
     *
     * @param name the name of the screen
     * @return true if a screen with the given name exists, false otherwise
     */
    boolean existsScreenByName(String name);

    /**
     * Check if a screen exists by its name and theater ID
     *
     * @param name      the name of the screen
     * @param theaterId the ID of the theater
     * @return true if a screen with the given name and theater ID exists, false otherwise
     */
    boolean existsByNameAndTheaterId(String name, Long theaterId);

    /**
     * Find all screens by theater ID
     *
     * @param theaterId the ID of the theater
     * @return list of screens associated with the given theater ID
     */
    List<Screen> findByTheaterId(Long theaterId);
}
