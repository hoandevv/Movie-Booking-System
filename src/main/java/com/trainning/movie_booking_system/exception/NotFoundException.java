package com.trainning.movie_booking_system.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BaseException {
    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String message, Object errors) {
        super(message, HttpStatus.NOT_FOUND, errors);
    }
}
