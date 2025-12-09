package com.trainning.movie_booking_system.dto.request.Movie;

import com.trainning.movie_booking_system.utils.enums.MovieStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMovieRequest {

    private String title;
    private String description;

    @Positive(message = "Duration must be greater than 0")
    private Integer duration;

    private String releaseDate; // ISO yyyy-MM-dd
    private String posterUrl;
    private String trailerUrl;

    @DecimalMin(value = "0.0", inclusive = true, message = "Rating must be >= 0")
    @DecimalMax(value = "10.0", inclusive = true, message = "Rating must be <= 10")
    private Double rating;

    private String genre;
    private String language;
    private MovieStatus status;
}
