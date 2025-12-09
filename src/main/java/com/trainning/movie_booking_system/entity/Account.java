package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.utils.enums.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.Set;

@Entity
@Table(
        name = "accounts",
        indexes = {
                @Index(name = "idx_account_username", columnList = "username"),
                @Index(name = "idx_account_email", columnList = "email")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account extends BaseEntity {

    @NotBlank(message = "Username must not be blank")
    @Size(min = 4, max = 30, message = "Username must be between 4 and 30 characters")
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Column(name = "password", nullable = false)
    @Setter
    private String password;
    
    @Setter
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status;

    @OneToMany(mappedBy = "account",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    @Builder.Default
    private Set<AccountHasRole> accountRoles = new java.util.HashSet<>();
}
