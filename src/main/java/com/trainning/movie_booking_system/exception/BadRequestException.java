package com.trainning.movie_booking_system.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }

    public BadRequestException(String message, Object error) {
        super(message, HttpStatus.BAD_REQUEST, error);
    }
}
