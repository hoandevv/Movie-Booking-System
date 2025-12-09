package com.trainning.movie_booking_system.security;

import com.trainning.movie_booking_system.entity.Account;
import com.trainning.movie_booking_system.entity.AccountHasRole;
import com.trainning.movie_booking_system.repository.AccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public record CustomUserDetailsService(AccountRepository accountRepository) implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        log.info("Loading UserDetails for {}", username);

        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));

        //check quyền
        Set<GrantedAuthority> authorities = new HashSet<>(account.getAccountRoles() != null ?
                account.getAccountRoles().stream()
                        .map(AccountHasRole::getRole)
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                        .collect(Collectors.toSet()) : Set.of());

        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        // Chuyển Account thành UserDetails
        return new CustomAccountDetails(account);
    }
}
