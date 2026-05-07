package com.greengo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greengo.domain.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM `user` WHERE id = #{id} FOR UPDATE")
    User selectByIdForUpdate(@Param("id") Long id);

    @Update("""
            UPDATE `user`
            SET wallet_balance = wallet_balance + #{amount}
            WHERE id = #{userId}
            """)
    int incrementWalletBalance(@Param("userId") Long userId,
                               @Param("amount") java.math.BigDecimal amount);

    @Update("""
            UPDATE `user`
            SET wallet_balance = wallet_balance - #{amount}
            WHERE id = #{userId}
              AND wallet_balance >= #{amount}
            """)
    int decrementWalletBalanceIfEnough(@Param("userId") Long userId,
                                       @Param("amount") java.math.BigDecimal amount);
}

