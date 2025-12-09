package com.trainning.movie_booking_system.mapper;

import com.trainning.movie_booking_system.dto.request.Voucher.CreateVoucherRequest;
import com.trainning.movie_booking_system.dto.request.Voucher.UpdateVoucherRequest;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherResponse;
import com.trainning.movie_booking_system.dto.response.Voucher.VoucherUsageResponse;
import com.trainning.movie_booking_system.entity.Voucher;
import com.trainning.movie_booking_system.entity.VoucherUsage;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper cho Voucher & VoucherUsage
 * Sử dụng MapStruct để generate implementation
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VoucherMapper {

    // ------------------------- CREATE -------------------------
    @Mapping(target = "currentUsageCount", constant = "0")
    @Mapping(target = "discountValue",
            expression = "java(request.getDiscountType() == com.trainning.movie_booking_system.utils.enums.DiscountType.BUY_X_GET_Y ? java.math.BigDecimal.ZERO : request.getDiscountValue())")
    @Mapping(target = "applicableMovieIds", ignore = true)
    @Mapping(target = "applicableTheaterIds", ignore = true)
    @Mapping(target = "applicableDaysOfWeek", ignore = true)
    @Mapping(target = "applicableTimeSlots", ignore = true)
    Voucher toEntity(CreateVoucherRequest request);

    // ------------------------- UPDATE -------------------------
    /**
     * Update Voucher entity từ UpdateVoucherRequest
     * Bỏ qua các giá trị null trong request
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "applicableMovieIds", ignore = true)
    @Mapping(target = "applicableTheaterIds", ignore = true)
    @Mapping(target = "applicableDaysOfWeek", ignore = true)
    @Mapping(target = "applicableTimeSlots", ignore = true)
    void updateEntityFromRequest(UpdateVoucherRequest request, @MappingTarget Voucher voucher);

    // ------------------------- RESPONSE -------------------------
    @Mapping(target = "applicableMovieIds", ignore = true)
    @Mapping(target = "applicableTheaterIds", ignore = true)
    @Mapping(target = "applicableDaysOfWeek", ignore = true)
    @Mapping(target = "applicableTimeSlots", ignore = true)
    @Mapping(target = "canUse", ignore = true)
    @Mapping(target = "userRemainingUsage", ignore = true)
    VoucherResponse toResponse(Voucher voucher);

    List<VoucherResponse> toResponseList(List<Voucher> vouchers);

    // ------------------------- VOUCHER USAGE -------------------------
    @Mapping(source = "voucher.id", target = "voucherId")
    @Mapping(source = "voucher.code", target = "voucherCode")
    @Mapping(source = "voucher.name", target = "voucherName")
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(source = "booking.id", target = "bookingId")
    @Mapping(source = "booking.status", target = "bookingStatus")
    VoucherUsageResponse toUsageResponse(VoucherUsage voucherUsage);

    List<VoucherUsageResponse> toUsageResponseList(List<VoucherUsage> voucherUsages);
}
