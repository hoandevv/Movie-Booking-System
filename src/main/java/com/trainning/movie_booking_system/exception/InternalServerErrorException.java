package com.trainning.movie_booking_system.exception;

import org.springframework.http.HttpStatus;
/**
 * Handler for 500 Internal Server Error exceptions.
 * chuẩn hóa xử lý lỗi hệ thống (HTTP 500) trong toàn bộ ứng dụng
* */
public class InternalServerErrorException extends BaseException {
    public InternalServerErrorException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
