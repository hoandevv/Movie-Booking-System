package com.trainning.movie_booking_system.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "account_roles",
        uniqueConstraints = @UniqueConstraint(columnNames = {"account_id", "role_id"}),
        indexes = {
                @Index(name = "idx_account_role_account_id", columnList = "account_id"),
                @Index(name = "idx_account_role_role_id", columnList = "role_id")
        }
)
@Getter
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AccountHasRole extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
}
