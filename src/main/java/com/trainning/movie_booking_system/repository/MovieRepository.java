package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long>, JpaSpecificationExecutor<Movie> {

    /**
     * Check if a movie exists by its title
     *
     * @param title the title of the movie
     * @return true if a movie with the given title exists, false otherwise
     */
    boolean existsByTitle(String title);

}
