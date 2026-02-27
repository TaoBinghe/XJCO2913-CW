package com.binghetao.controller;

import com.binghetao.domain.Result;
import com.binghetao.domain.Scooter;
import com.binghetao.service.ScooterService;
import com.binghetao.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/scooter")
public class AdminScooterController {

    @Autowired
    private ScooterService scooterService;

    private boolean isAdmin() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null) {
            return false;
        }
        Object role = claims.get("role");
        return role != null && "MANAGER".equalsIgnoreCase(role.toString());
    }

    @PostMapping("/add")
    public Result<?> add(@RequestBody Scooter scooter) {
        if (!isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        boolean ok = scooterService.addScooter(scooter);
        if (ok) {
            return Result.success();
        }
        return Result.error("Failed to add scooter, code may already exist");
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody Scooter scooter) {
        if (!isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        boolean ok = scooterService.updateScooter(scooter);
        if (ok) {
            return Result.success();
        }
        return Result.error("Failed to update scooter, record not found or code duplicated");
    }

    @DeleteMapping("/delete")
    public Result<?> delete(@RequestParam Long id) {
        if (!isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        boolean ok = scooterService.deleteScooter(id);
        if (ok) {
            return Result.success();
        }
        return Result.error("Failed to delete scooter, record not found");
    }
}

