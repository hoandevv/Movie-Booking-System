package com.trainning.movie_booking_system.mapper;

import com.trainning.movie_booking_system.dto.response.Admin.UserAdminResponse;
import com.trainning.movie_booking_system.entity.Account;
import com.trainning.movie_booking_system.entity.AccountHasRole;
import com.trainning.movie_booking_system.entity.User;
import com.trainning.movie_booking_system.utils.enums.RoleType;

public class UserAdminMapper {

    public static UserAdminResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        Account account = user.getAccount();
        if (account == null) {
            throw new IllegalArgumentException("User must have an associated account");
        }

        // Build full name
        String firstName = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String lastName = user.getLastName() != null ? user.getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();

        // Check role staff
        boolean isStaff = false;
        if (account.getAccountRoles() != null) {
            isStaff = account.getAccountRoles().stream()
                    .map(AccountHasRole::getRole)
                    .filter(role -> role != null && role.getName() != null)
                    .anyMatch(role -> role.getName() == RoleType.STAFF);
        }

        return UserAdminResponse.builder()
                .userId(user.getId())
                .accountId(account.getId())
                .username(account.getUsername())
                .email(account.getEmail())
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(user.getPhoneNumber())
                .status(account.getStatus() != null ? account.getStatus().name() : null)
                .isStaff(isStaff)
                .build();
    }
}
