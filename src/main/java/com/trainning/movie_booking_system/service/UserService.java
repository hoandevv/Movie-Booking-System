package com.trainning.movie_booking_system.service;

import com.trainning.movie_booking_system.entity.User;

public interface UserService {
    /**
     * Sử dụng để tạo một user mới
     * @param user thông tin cần tạo
     */
    void createUser(User user);
}
