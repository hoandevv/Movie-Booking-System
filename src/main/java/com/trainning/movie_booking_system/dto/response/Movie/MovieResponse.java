package com.trainning.movie_booking_system.dto.response.Movie;

import com.trainning.movie_booking_system.utils.enums.MovieStatus;
import lombok.Builder;
import lombok.Getter;
import java.io.Serializable;

@Getter
@Builder
public class MovieResponse implements Serializable {
    private Long id;
    private String title;
    private String description;
    private Integer duration;
    private String releaseDate;
    private String posterUrl;
    private String trailerUrl;
    private Double rating;
    private String genre;
    private String language;
    private MovieStatus status;
}
