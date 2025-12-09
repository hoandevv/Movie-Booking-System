package com.trainning.movie_booking_system.dto.response.Voucher;

import lombok.*;

import java.math.BigDecimal;

/**
 * Response DTO for voucher validation result
 * Contains validation status and calculated discount
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherValidationResult {

    /**
     * Voucher hợp lệ hay không
     */
    private Boolean isValid;

    /**
     * Thông báo chi tiết về kết quả kiểm tra voucher (vd: "Voucher hết hạn", "Voucher hợp lệ")
     */
    private String message;

    /**
     * Mã voucher
     */
    private String voucherCode;

    /**
     * Tên voucher
     */
    private String voucherName;

    /**
     * Giá trị ban đầu của booking trước khi áp voucher
     */
    private BigDecimal originalAmount;

    /**
     * Số tiền giảm giá được áp dụng từ voucher
     */
    private BigDecimal discountAmount;

    /**
     * Giá cuối cùng sau khi áp voucher
     */
    private BigDecimal finalAmount;

    /**
     * Số lượt sử dụng còn lại của user đối với voucher này
     */
    private Integer remainingUsage;

    /**
     * Thời hạn hiệu lực của voucher (string dạng yyyy-MM-dd hoặc ISO)
     */
    private String validUntil;

}
