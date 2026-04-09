package com.greengo.controller;

import com.greengo.domain.Result;
import com.greengo.domain.Scooter;
import com.greengo.service.GeoAddressService;
import com.greengo.service.ScooterService;
import com.greengo.utils.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

// Admin scooter CRUD API
@RestController
@RequestMapping("/admin/scooter")
public class AdminScooterController {

    @Autowired
    private ScooterService scooterService;
    @Autowired
    private GeoAddressService geoAddressService;

    @GetMapping("/list")
    public Result<List<Scooter>> list() {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        List<Scooter> scooters = scooterService.listAll();
        return Result.success(scooters);
    }

    @GetMapping("/resolve-location")
    public Result<String> resolveLocation(@RequestParam BigDecimal longitude, @RequestParam BigDecimal latitude) {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        String location = geoAddressService.reverseGeocode(longitude, latitude);
        if (location == null || location.isBlank()) {
            return Result.error("Failed to resolve address from coordinates");
        }
        return Result.success(location);
    }

    @PostMapping("/add")
    public Result<?> add(@RequestBody Scooter scooter) {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        boolean ok = scooterService.addScooter(scooter);
        if (ok) {
            return Result.success();
        }
        return Result.error("Failed to add scooter, code may already exist or coordinates/address is invalid");
    }

    // Update scooter by id
    @PostMapping("/update")
    public Result<?> update(@RequestBody Scooter scooter) {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        boolean ok = scooterService.updateScooter(scooter);
        if (ok) {
            return Result.success();
        }
        return Result.error("Failed to update scooter, record not found, code duplicated, or coordinates/address is invalid");
    }

    // Delete scooter by id
    @DeleteMapping("/delete")
    public Result<?> delete(@RequestParam Long id) {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        boolean ok = scooterService.deleteScooter(id);
        if (ok) {
            return Result.success();
        }
        return Result.error("Failed to delete scooter, record not found");
    }
}


