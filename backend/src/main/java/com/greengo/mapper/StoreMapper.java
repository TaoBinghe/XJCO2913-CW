package com.greengo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greengo.domain.Store;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface StoreMapper extends BaseMapper<Store> {

    @Select("SELECT * FROM store WHERE id = #{id} FOR UPDATE")
    Store selectByIdForUpdate(@Param("id") Long id);
}
