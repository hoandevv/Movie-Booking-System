package com.trainning.movie_booking_system.dto.request.Movie;

import com.trainning.movie_booking_system.utils.enums.MovieStatus;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieRequest {

    @NotBlank(message = "Title is mandatory")
    private String title;

    private String description;

    @NotNull(message = "Duration is mandatory")
    @Positive(message = "Duration must be greater than 0")
    private Integer duration;

    private String releaseDate;

    // Thêm để FE biết thời gian bắt đầu / kết thúc lịch chiếu
    private String screeningStartDate;
    private String screeningEndDate;

    //Thêm để FE biết giới hạn giờ chiếu
    private String allowedStartTime;
    private String allowedEndTime;

    private String posterUrl;
    private String trailerUrl;

    @DecimalMin(value = "0.0", inclusive = true, message = "Rating must be >= 0")
    @DecimalMax(value = "10.0", inclusive = true, message = "Rating must be <= 10")
    private Double rating;

    private String genre;
    private String language;

    @NotNull(message = "Status is mandatory")
    private MovieStatus status;
}
