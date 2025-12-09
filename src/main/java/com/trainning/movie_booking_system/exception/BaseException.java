package com.trainning.movie_booking_system.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BaseException extends RuntimeException {

    private final HttpStatus status;
    private final Object errors;

    public BaseException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errors = null;
    }

    public BaseException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errors = null;
    }

    public BaseException(String message, HttpStatus status, Object errors) {
        super(message);
        this.status = status;
        this.errors = errors;
    }
}
