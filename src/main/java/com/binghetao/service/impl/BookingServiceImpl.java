package com.binghetao.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.binghetao.domain.Booking;
import com.binghetao.domain.PricingPlan;
import com.binghetao.mapper.BookingMapper;
import com.binghetao.mapper.PricingPlanMapper;
import com.binghetao.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookingServiceImpl extends ServiceImpl<BookingMapper, Booking> implements BookingService {

    @Autowired
    private PricingPlanMapper pricingPlanMapper;

    @Override
    public List<PricingPlan> listPricingPlan() {
        return pricingPlanMapper.selectList(null);
    }
}
