package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.entity.User;
import com.trainning.movie_booking_system.repository.UserRepository;
import com.trainning.movie_booking_system.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public void createUser(User user) {

    }
}
