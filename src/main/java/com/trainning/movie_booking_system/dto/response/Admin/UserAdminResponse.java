package com.trainning.movie_booking_system.dto.response.Admin;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserAdminResponse {
    private Long userId;
    private Long accountId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private boolean emailVerified;
    private String status;
    private boolean isStaff;

    private String createdAt;
}
