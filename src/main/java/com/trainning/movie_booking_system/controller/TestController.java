package com.trainning.movie_booking_system.controller;

import com.trainning.movie_booking_system.dto.response.System.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/protected")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    /**
     * Endpoint bảo vệ - cần JWT token
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
    @GetMapping("/user-info")
    public ResponseEntity<BaseResponse<String>> getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        String username = authentication.getName();
        String authorities = authentication.getAuthorities().toString();
        
        String userInfo = String.format("Username: %s, Authorities: %s", username, authorities);
        
        return ResponseEntity.ok(BaseResponse.<String>builder()
                .success(true)
                .message("Lấy thông tin user thành công")
                .data(userInfo)
                .build());
    }

    /**
     * Endpoint bảo vệ khác
     */
    @GetMapping("/admin-only")
    public ResponseEntity<BaseResponse<String>> adminOnly() {
        return ResponseEntity.ok(BaseResponse.<String>builder()
                .success(true)
                .message("Chỉ admin mới có thể truy cập endpoint này")
                .data("Bạn có quyền admin!")
                .build());
    }
}
