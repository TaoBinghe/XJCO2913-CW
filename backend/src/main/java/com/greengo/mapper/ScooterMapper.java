package com.greengo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greengo.domain.Scooter;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ScooterMapper extends BaseMapper<Scooter> {

    @Update("""
            UPDATE scooter
            SET status = #{nextStatus},
                lock_status = #{nextLockStatus}
            WHERE id = #{id}
              AND rental_mode = #{rentalMode}
              AND status = #{expectedStatus}
            """)
    int updateStatusAndLockIfCurrent(@Param("id") Long id,
                                     @Param("rentalMode") String rentalMode,
                                     @Param("expectedStatus") String expectedStatus,
                                     @Param("nextStatus") String nextStatus,
                                     @Param("nextLockStatus") String nextLockStatus);
}


