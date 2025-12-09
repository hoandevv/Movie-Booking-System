package com.trainning.movie_booking_system.service.impl;

import com.trainning.movie_booking_system.dto.request.Admin.CreateUserRequest;
import com.trainning.movie_booking_system.dto.request.Admin.UpdateUserRequest;
import com.trainning.movie_booking_system.dto.response.Admin.UserAdminResponse;
import com.trainning.movie_booking_system.entity.*;
import com.trainning.movie_booking_system.exception.AlreadyExistsException;
import com.trainning.movie_booking_system.exception.BadRequestException;
import com.trainning.movie_booking_system.exception.NotFoundException;
import com.trainning.movie_booking_system.mapper.UserAdminMapper;
import com.trainning.movie_booking_system.repository.*;
import com.trainning.movie_booking_system.service.AdminUserService;
import com.trainning.movie_booking_system.utils.enums.RoleType;
import com.trainning.movie_booking_system.utils.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // UTILITY
    private Account getAccountOrThrow(Long userId) {
        if (userId == null) throw new IllegalArgumentException("User ID cannot be null");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));
        Account account = user.getAccount();
        if (account == null) throw new IllegalStateException("User account not found for id: " + userId);
        return account;
    }

    private void assignRole(Account account, RoleType roleType) {
        Role role = roleRepository.findByName(roleType)
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleType.name()));
        account.getAccountRoles().clear();
        AccountHasRole link = new AccountHasRole();
        link.setAccount(account);
        link.setRole(role);
        account.getAccountRoles().add(link);
    }

    // CREATE USER
    @Override
    @Transactional
    public UserAdminResponse createUser(CreateUserRequest request) {
        if (request == null) throw new BadRequestException("Request cannot be null");

        String email = request.getEmail().trim();
        String username = request.getUsername().trim();
        String password = request.getPassword().trim();

        if (accountRepository.existsByEmail(email)) throw new AlreadyExistsException("Email already used");
        if (accountRepository.existsByUsername(username)) throw new AlreadyExistsException("Username already used");

        Account account = new Account();
        account.setEmail(email);
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password));
        account.setStatus(UserStatus.ACTIVE);
        account.setEmailVerified(true);

        RoleType roleType = Boolean.TRUE.equals(request.getIsStaff()) ? RoleType.STAFF : RoleType.USER;
        assignRole(account, roleType);

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAccount(account);

        accountRepository.save(account);
        userRepository.save(user);

        return UserAdminMapper.toResponse(user);
    }

    // GET USER
    @Override
    @Transactional(readOnly = true)
    public UserAdminResponse getUserById(Long id) {
        if (id == null) throw new BadRequestException("User ID cannot be null");
        return userRepository.findById(id)
                .map(UserAdminMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    // UPDATE USER
    @Override
    @Transactional
    public UserAdminResponse updateUser(Long id, UpdateUserRequest request) {
        if (id == null) throw new BadRequestException("User ID cannot be null");
        if (request == null) throw new BadRequestException("Update request cannot be null");

        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
        Account account = user.getAccount();

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());

        if (request.getIsActive() != null) {
            account.setStatus(request.getIsActive() ? UserStatus.ACTIVE : UserStatus.INACTIVE);
        }

        if (request.getIsStaff() != null) {
            RoleType newRole = request.getIsStaff() ? RoleType.STAFF : RoleType.USER;
            assignRole(account, newRole);
        }

        userRepository.save(user);
        return UserAdminMapper.toResponse(user);
    }

    // STATUS MANAGEMENT
    @Override
    @Transactional
    public void activateUser(Long id, Long adminId) {
        Account account = getAccountOrThrow(id);
        if (account.getStatus() == UserStatus.ACTIVE) {
            throw new BadRequestException("User is already active");
        }
        account.setStatus(UserStatus.ACTIVE);
        log.info("Admin {} activated user {}", adminId, id);
    }

    @Override
    @Transactional
    public void deactivateUser(Long id) {
        Account account = getAccountOrThrow(id);
        if (account.getStatus() == UserStatus.INACTIVE) {
            throw new BadRequestException("User is already inactive");
        }
        account.setStatus(UserStatus.INACTIVE);
        log.info("User {} deactivated", id);
    }

    @Override
    @Transactional
    public void lockUser(Long id) {
        Account account = getAccountOrThrow(id);
        if (account.getStatus() == UserStatus.LOCKED) {
            throw new BadRequestException("User is already locked");
        }
        account.setStatus(UserStatus.LOCKED);
        log.info("User {} locked", id);
    }

    @Override
    @Transactional
    public void unlockUser(Long id) {
        Account account = getAccountOrThrow(id);
        if (account.getStatus() != UserStatus.LOCKED) {
            throw new BadRequestException("User is not locked, cannot unlock");
        }
        account.setStatus(UserStatus.ACTIVE);
        log.info("User {} unlocked", id);
    }

    // GET ALL USERS
    @Override
    @Transactional(readOnly = true)
    public Page<UserAdminResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserAdminMapper::toResponse);
    }
}
