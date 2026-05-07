package com.greengo.service;

import com.greengo.domain.AdminDailyRevenueSummary;
import com.greengo.domain.AdminWeeklyRevenueSummary;

public interface AdminRevenueService {

    AdminWeeklyRevenueSummary getWeeklyRevenueSummary();

    AdminDailyRevenueSummary getDailyRevenueSummary();
}

