package com.trainning.movie_booking_system.entity;

import com.trainning.movie_booking_system.utils.enums.TheaterStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "theaters",
        indexes = {
        @Index(name = "idx_theater_name", columnList = "name"),
        @Index(name = "idx_theater_city", columnList = "city"),
        @Index(name = "idx_theater_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Theater extends BaseEntity {
    @Column(nullable = false, length = 150, unique = true)
    private String name;

    @Column(length = 255)
    private String location;

    @Column(length = 100)
    private String city;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TheaterStatus status;
}

