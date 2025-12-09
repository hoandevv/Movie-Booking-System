package com.trainning.movie_booking_system.dto.request.Movie;

import com.trainning.movie_booking_system.utils.enums.MovieStatus;
import lombok.Data;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
public class MovieSearchFilter {
    private String keyword;
    private Set<String> genres;
    private String language;
    private MovieStatus status;

    private BigDecimal ratingMin;
    private BigDecimal ratingMax;

    private Integer durationMin;
    private Integer durationMax;

    private LocalDate releaseFrom;
    private LocalDate releaseTo;

    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "releaseDate";
    private Sort.Direction direction = Sort.Direction.DESC;
}
