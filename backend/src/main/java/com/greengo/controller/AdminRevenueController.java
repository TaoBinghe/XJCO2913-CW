package com.greengo.controller;

import com.greengo.domain.AdminWeeklyRevenueSummary;
import com.greengo.domain.Result;
import com.greengo.service.AdminRevenueService;
import com.greengo.utils.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/revenue")
public class AdminRevenueController {

    @Autowired
    private AdminRevenueService adminRevenueService;

    @GetMapping("/weekly")
    public Result<AdminWeeklyRevenueSummary> weeklyRevenue() {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        return Result.success(adminRevenueService.getWeeklyRevenueSummary());
    }
}

