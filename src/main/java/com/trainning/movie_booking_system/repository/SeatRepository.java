package com.trainning.movie_booking_system.repository;

import com.trainning.movie_booking_system.entity.Seat;
import com.trainning.movie_booking_system.utils.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    /*
    * Soft delete
    * Không hiển thị dữ liệu đã bị xóa,
    * ví dụ: khi tìm kiếm ghế theo screenId, chỉ trả về những ghế chưa bị xóa mềm (isDeleted = false)
    * Không hiển thị dữ liệu đã bị xóa,
    * Không hiển thị dữ liệu đã bị xóa,
    * */
    @Query("SELECT s FROM Seat s WHERE s.screen.id = :screenId AND s.isDeleted = false")
    List<Seat> findByScreenId(@Param("screenId") Long screenId);

    @Query("SELECT s FROM Seat s WHERE s.screen.id = :screenId AND s.status = :status AND s.isDeleted = false")
    List<Seat> findByScreenIdAndStatus(@Param("screenId") Long screenId, @Param("status") SeatStatus status);

    @Query("SELECT s FROM Seat s WHERE s.screen.id = :screenId AND s.rowLabel = :rowLabel AND s.seatNumber = :seatNumber AND s.isDeleted = false")
    Optional<Seat> findByScreenIdAndRowLabelAndSeatNumber(
            @Param("screenId") Long screenId,
            @Param("rowLabel") String rowLabel,
            @Param("seatNumber") int seatNumber
    );

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
            "FROM Seat s WHERE s.screen.id = :screenId AND s.rowLabel = :rowLabel AND s.seatNumber = :seatNumber AND s.isDeleted = false")
    boolean existsByScreenIdAndRowLabelAndSeatNumber(
            @Param("screenId") Long screenId,
            @Param("rowLabel") String rowLabel,
            @Param("seatNumber") int seatNumber
    );

    @Query("SELECT s FROM Seat s WHERE s.screen.id = :screenId AND s.isDeleted = false ORDER BY s.rowLabel, s.seatNumber")
    List<Seat> findAllByScreenIdOrderByRowLabelAndSeatNumber(@Param("screenId") Long screenId);

    /**
     * Check existence of any seats for a given screen id
     */
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Seat s WHERE s.screen.id = :screenId AND s.isDeleted = false")
    boolean existsByScreen_Id(@Param("screenId") Long screenId);


    // Nếu bạn muốn truy cập danh sách ghế đã bị xóa mềm
    @Query("SELECT s FROM Seat s WHERE s.screen.id = :screenId AND s.isDeleted = true ORDER BY s.rowLabel, s.seatNumber")
    List<Seat> findDeletedByScreenId(@Param("screenId") Long screenId);
}
