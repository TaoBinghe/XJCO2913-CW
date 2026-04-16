package com.greengo.controller;

import com.greengo.domain.Result;
import com.greengo.domain.Store;
import com.greengo.service.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/store")
public class StoreController {

    @Autowired
    private StoreService storeService;

    @GetMapping("/list")
    public Result<?> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime appointmentStart,
            @RequestParam(required = false) String hiredPeriod) {
        try {
            List<Store> stores = storeService.listEnabledStores(appointmentStart, hiredPeriod);
            return Result.success(stores);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/{storeId}")
    public Result<?> detail(
            @PathVariable Long storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime appointmentStart,
            @RequestParam(required = false) String hiredPeriod) {
        try {
            Store store = storeService.getEnabledStore(storeId, appointmentStart, hiredPeriod);
            if (store == null) {
                return Result.error("Store not found");
            }
            return Result.success(store);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
