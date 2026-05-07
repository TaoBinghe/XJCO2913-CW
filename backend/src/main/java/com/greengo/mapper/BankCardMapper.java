package com.greengo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greengo.domain.BankCard;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface BankCardMapper extends BaseMapper<BankCard> {

    @Update("""
            UPDATE bank_card
            SET card_number = NULL,
                card_last_four = #{card.cardLastFour},
                card_fingerprint = #{card.cardFingerprint},
                password_hash = #{card.passwordHash}
            WHERE id = #{card.id}
            """)
    int upgradeSecurity(@Param("card") BankCard card);
}
