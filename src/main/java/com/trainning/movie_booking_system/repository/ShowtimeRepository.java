package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Movie;
import com.trainning.movie_booking_system.entity.Showtime;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    /**
     * Check if a showtime exists for a given screen, date, and start time.
     *
     * @param screenId the ID of the screen
     * @param showDate the date of the show
     * @param startTime the start time of the show
     * @return true if a showtime exists, false otherwise
     */
    boolean existsByScreenIdAndShowDateAndStartTime(Long screenId, LocalDate showDate, LocalTime startTime);

    /**
     * Check if a showtime exists for a given screen, date, and start time,
     * excluding a specific showtime ID.
     *
     * @param screen_id the ID of the screen
     * @param showDate the date of the show
     * @param startTime the start time of the show
     * @param id the ID of the showtime to exclude
     * @return true if a showtime exists, false otherwise
     */
    boolean existsByScreenIdAndShowDateAndStartTimeAndIdNot(Long screen_id, LocalDate showDate, LocalTime startTime, Long id);

    /**
     * Find movies being shown in a specific theater on a specific date.
     *
     * @param theaterId the ID of the theater
     * @param date the date to check for showtimes
     * @return a list of movies being shown
     */
    @Query("""
    SELECT DISTINCT s.movie
    FROM Showtime s
    JOIN s.screen sc
    JOIN sc.theater t
    WHERE t.id = :theaterId
      AND s.showDate = :date
      AND s.status = "ACTIVE"
    """)
    List<Movie> findMoviesByTheaterAndDate(@Param("theaterId") Long theaterId,
                                           @Param("date") LocalDate date);


    /**
     * Find showtimes for a specific theater, movie, and date.
     * @param theaterId the ID of the theater
     * @param movieId the ID of the movie
     * @param date the date to check for showtimes
     * @return a list of showtimes
     */
    @Query("""
    SELECT s
    FROM Showtime s
    JOIN FETCH s.screen sc
    JOIN FETCH sc.theater t
    WHERE t.id = :theaterId
      AND s.movie.id = :movieId
      AND s.showDate = :date
      AND s.status = "ACTIVE"
    ORDER BY sc.name, s.startTime
    """)
    List<Showtime> findShowtimesByTheaterAndMovieAndDate(
            @Param("theaterId") Long theaterId,
            @Param("movieId") Long movieId,
            @Param("date") LocalDate date);

}
