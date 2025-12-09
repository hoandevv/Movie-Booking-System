package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    @EntityGraph(attributePaths = {"accountRoles", "accountRoles.role"})
    Optional<Account> findByUsername(String username);

    /**
     *
     * @param email
     * @return
     */
    Optional<Account> findByEmail(String email);
    
    /**
     * Kiểm tra email đã tồn tại chưa
     * @param email email cần kiểm tra
     * @return true nếu email đã tồn tại, false nếu chưa
     */
    boolean existsByEmail(String email);
    
    /**
     * Kiểm tra username đã tồn tại chưa
     * @param username username cần kiểm tra
     * @return true nếu username đã tồn tại, false nếu chưa
     */
    boolean existsByUsername(String username);
}
