package com.trainning.movie_booking_system.service;


import com.trainning.movie_booking_system.dto.request.Admin.CreateUserRequest;
import com.trainning.movie_booking_system.dto.request.Admin.UpdateUserRequest;
import com.trainning.movie_booking_system.dto.response.Admin.UserAdminResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    UserAdminResponse createUser(CreateUserRequest request);

    UserAdminResponse getUserById(Long id);

    UserAdminResponse updateUser(Long id, UpdateUserRequest request);

    void deactivateUser(Long id);

    void activateUser(Long id, Long adminId);

    void lockUser(Long id);

    void unlockUser(Long id);


    Page<UserAdminResponse> getAllUsers(Pageable pageable);
}
