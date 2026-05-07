package com.greengo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greengo.domain.Booking;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

public interface BookingMapper extends BaseMapper<Booking> {

    @Select("SELECT * FROM booking WHERE id = #{id} FOR UPDATE")
    Booking selectByIdForUpdate(@Param("id") Long id);

    @Update("""
            UPDATE booking
            SET status = #{nextStatus}
            WHERE id = #{id}
              AND rental_type = #{rentalType}
              AND status = #{currentStatus}
              AND pickup_deadline < #{now}
            """)
    int expireReservationIfStillReserved(@Param("id") Long id,
                                         @Param("rentalType") String rentalType,
                                         @Param("currentStatus") String currentStatus,
                                         @Param("nextStatus") String nextStatus,
                                         @Param("now") LocalDateTime now);

    @Update("""
            UPDATE booking
            SET status = #{nextStatus}
            WHERE id = #{id}
              AND rental_type = #{rentalType}
              AND status = #{currentStatus}
              AND end_time < #{now}
              AND return_time IS NULL
            """)
    int markOverdueIfStillInProgress(@Param("id") Long id,
                                     @Param("rentalType") String rentalType,
                                     @Param("currentStatus") String currentStatus,
                                     @Param("nextStatus") String nextStatus,
                                     @Param("now") LocalDateTime now);
}

