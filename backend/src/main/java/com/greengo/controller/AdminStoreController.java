package com.greengo.controller;

import com.greengo.domain.Result;
import com.greengo.domain.Store;
import com.greengo.service.StoreService;
import com.greengo.utils.AuthUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/stores")
public class AdminStoreController {

    @Autowired
    private StoreService storeService;

    @GetMapping
    public Result<List<Store>> list() {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        return Result.success(storeService.listAllStores());
    }

    @GetMapping("/{id}")
    public Result<?> getById(@PathVariable Long id) {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        Store store = storeService.getStoreById(id);
        if (store == null) {
            return Result.error("Store not found");
        }
        return Result.success(store);
    }

    @PostMapping
    public Result<?> create(@RequestBody Store store) {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        try {
            boolean created = storeService.createStore(store);
            return created ? Result.success() : Result.error("Failed to create store");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Store store) {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        try {
            boolean updated = storeService.updateStore(id, store);
            return updated ? Result.success() : Result.error("Failed to update store");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        if (!AuthUtil.isAdmin()) {
            return Result.error("Forbidden: admin only");
        }
        boolean deleted = storeService.deleteStore(id);
        return deleted ? Result.success() : Result.error("Failed to delete store");
    }
}
