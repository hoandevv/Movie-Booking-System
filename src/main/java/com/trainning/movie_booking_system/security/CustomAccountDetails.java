package com.trainning.movie_booking_system.security;

import com.trainning.movie_booking_system.entity.Account;
import com.trainning.movie_booking_system.utils.enums.UserStatus;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

/**
 * Custom UserDetails implementation cho Account entity
 * Chuyển đổi Account thành UserDetails để Spring Security có thể xử lý
 */
@Getter
@Setter
public class CustomAccountDetails implements UserDetails {

    private Account account;

    public CustomAccountDetails(Account account) {
        this.account = account;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Lấy roles từ AccountHasRole và chuyển thành GrantedAuthority
        if (account.getAccountRoles() == null || account.getAccountRoles().isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return account.getAccountRoles().stream()
                .map(accountRole -> new SimpleGrantedAuthority("ROLE_" + accountRole.getRole().getName()))
                .toList();
    }

    @Override
    public String getPassword() {
        return account.getPassword();
    }

    @Override
    public String getUsername() {
        return account.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Account không bao giờ hết hạn
    }

    @Override
    public boolean isAccountNonLocked() {
        return account.getStatus() != null && account.getStatus() != UserStatus.LOCKED;
    }


    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Credentials không bao giờ hết hạn
    }

    @Override
    public boolean isEnabled() {
        // Chỉ ACTIVE và email đã verify mới là enabled
        return account.getStatus() == UserStatus.ACTIVE && account.isEmailVerified();
    }
    /**
     * Getter để truy cập Account entity nếu cần
     */
    public Account account() {
        return account;
    }
}
