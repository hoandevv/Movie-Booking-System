package com.trainning.movie_booking_system.dto.response.Showtime;

import lombok.Builder;
import lombok.Getter;
import java.io.Serializable;
import java.util.List;

@Getter
@Builder
public class ShowtimeByScreenResponse implements Serializable {
    private String screenName;
    private List<ShowtimeSlotResponse> showtimes;

    @Getter
    @Builder
    public static class ShowtimeSlotResponse {
        private Long id;
        private String startTime;
        private String endTime;
        private Double price;
    }
}

