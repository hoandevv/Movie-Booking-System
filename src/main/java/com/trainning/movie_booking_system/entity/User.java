package com.trainning.movie_booking_system.entity;

    import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
    import jakarta.validation.constraints.Pattern;
    import jakarta.validation.constraints.Size;
    import lombok.*;

@Entity
@Table(
        name = "user"
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Size(max = 50, message = "First name must not exceed 50 characters")
    @Column(name = "first_name", length = 50)
    private String firstName;


    @Size(max = 50, message = "Last name must not exceed 50 characters")
    @Column(name = "last_name", length = 50)
    private String lastName;

    @Pattern(
            regexp = "^(\\+?84|0)(3|5|7|8|9)[0-9]{8}$",
            message = "Invalid Vietnamese phone number format"
    )
    @Column(name = "phone_number", length = 15)
    private String phoneNumber;
    
    @OneToOne
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;
}
