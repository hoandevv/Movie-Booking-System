package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.utils.enums.MovieStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "movies",
        indexes = {
                @Index(name = "idx_movie_title", columnList = "title"),
                @Index(name = "idx_movie_status", columnList = "status"),
                @Index(name = "idx_movie_genre", columnList = "genre"),
                @Index(name = "idx_movie_language", columnList = "language"),
                @Index(name = "idx_movie_release_date", columnList = "release_date")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Movie extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description; 

    @Column(nullable = false)
    private Integer duration;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(name = "poster_url", columnDefinition = "TEXT")
    private String posterUrl;

    @Column(name = "trailer_url", columnDefinition = "TEXT")
    private String trailerUrl;

    @Column(precision = 3, scale = 1)
    private BigDecimal rating;

    @Column(length = 100)
    private String genre;

    @Column(length = 50)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovieStatus status;
}

