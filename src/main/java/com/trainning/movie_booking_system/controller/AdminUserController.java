package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.request.Admin.CreateUserRequest;
import com.trainning.movie_booking_system.dto.request.Admin.UpdateUserRequest;
import com.trainning.movie_booking_system.dto.response.Admin.UserAdminResponse;
import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import com.trainning.movie_booking_system.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final AdminUserService adminUserService;

    @PostMapping
    public ResponseEntity<BaseResponse<UserAdminResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("[ADMIN] API create user: {}", request.getUsername());
        UserAdminResponse user = adminUserService.createUser(request);
        return ResponseEntity.ok(BaseResponse.success(user, "User created successfully"));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<BaseResponse<UserAdminResponse>> getUserById(@PathVariable Long userId) {
        log.info("[ADMIN] API get user by id: {}", userId);
        UserAdminResponse user = adminUserService.getUserById(userId);
        return ResponseEntity.ok(BaseResponse.success(user, "User retrieved successfully"));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<BaseResponse<UserAdminResponse>> updateUser(@PathVariable Long userId,
                                                                      @Valid @RequestBody UpdateUserRequest request) {
        log.info("[ADMIN] API update user id: {}", userId);
        UserAdminResponse user = adminUserService.updateUser(userId, request);
        return ResponseEntity.ok(BaseResponse.success(user, "User updated successfully"));
    }
    @GetMapping
    public ResponseEntity<BaseResponse<Page<UserAdminResponse>>> getAllUsers(Pageable pageable) {
        log.info("[ADMIN] API get all users");
        Page<UserAdminResponse> users = adminUserService.getAllUsers(pageable);
        return ResponseEntity.ok(BaseResponse.success(users, "Users retrieved successfully"));
    }
    @PutMapping("/{id}/activate")
    public ResponseEntity<BaseResponse<String>> activateUser(@PathVariable Long id, @RequestParam(required = false) Long adminId) {
        adminUserService.activateUser(id, adminId);
        return ResponseEntity.ok(BaseResponse.success(null, "User activated successfully"));
    }
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<BaseResponse<String>> deactivateUser(@PathVariable Long id) {
        adminUserService.deactivateUser(id);
        return ResponseEntity.ok(BaseResponse.success(null, "User deactivated successfully"));
    }
    @PutMapping("/{id}/lock")
    public ResponseEntity<BaseResponse<String>> lockUser(@PathVariable Long id) {
        adminUserService.lockUser(id);
        return ResponseEntity.ok(BaseResponse.success(null, "User locked successfully"));
    }
    @PutMapping("/{id}/unlock")
    public ResponseEntity<BaseResponse<String>> unlockUser(@PathVariable Long id) {
        adminUserService.unlockUser(id);
        return ResponseEntity.ok(BaseResponse.success(null, "User unlocked successfully"));
    }

}
