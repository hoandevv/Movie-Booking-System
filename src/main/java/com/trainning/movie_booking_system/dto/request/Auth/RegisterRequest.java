package com.trainning.movie_booking_system.dto.request.Auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Username not blank")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Email not blank")
    @Email(message = "Email has invalid format")
    private String email;

    @NotBlank(message = "Password not blank")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must be at least 8 characters and contain uppercase, lowercase, digit, and special character"
    )
    private String password;

    @NotBlank(message = "First name not blank")
    @Size(min = 2, max = 50, message = "First name must be 2 characters long")
    private String firstName;

    @NotBlank(message = "Last name not blank")
    @Size(min = 2, max = 50, message = "Last name must be 2 characters long")
    private String lastName;

    @NotBlank(message = "Phone number not blank")
    @Size(min = 10, max = 15, message = "Phone number must be 10 characters long")
    private String phoneNumber;
}


