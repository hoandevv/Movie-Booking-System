package com.trainning.movie_booking_system.utils.enums;

import lombok.Getter;
import java.util.Arrays;

@Getter
public enum MovieSortField {
    title("title"),
    releaseDate("releaseDate"),
    rating("rating"),
    duration("duration"),
    genre("genre");

    private final String field;

    MovieSortField(String f) { this.field = f; }

    public static String safe(String input) {
        return Arrays.stream(values())
                .map(MovieSortField::getField)
                .filter(f -> f.equalsIgnoreCase(input))
                .findFirst()
                .orElse("releaseDate");
    }
}
