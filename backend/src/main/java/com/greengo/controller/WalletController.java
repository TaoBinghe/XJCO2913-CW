package com.greengo.controller;

import com.greengo.domain.BankCardBindRequest;
import com.greengo.domain.Result;
import com.greengo.domain.WalletRechargeRequest;
import com.greengo.service.BankCardService;
import com.greengo.service.WalletService;
import com.greengo.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private WalletService walletService;

    @Autowired
    private BankCardService bankCardService;

    @GetMapping
    public Result<?> summary() {
        Long userId = currentUserId();
        if (userId == null) {
            return Result.error("Unauthorized");
        }
        try {
            return Result.success(walletService.getSummary(userId));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/cards")
    public Result<?> bindCard(@RequestBody BankCardBindRequest request) {
        Long userId = currentUserId();
        if (userId == null) {
            return Result.error("Unauthorized");
        }
        try {
            return Result.success(bankCardService.bindCard(userId, request));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/recharge")
    public Result<?> recharge(@RequestBody WalletRechargeRequest request) {
        Long userId = currentUserId();
        if (userId == null) {
            return Result.error("Unauthorized");
        }
        if (request == null || request.getCardId() == null || request.getAmount() == null) {
            return Result.error("Card id and recharge amount are required");
        }
        try {
            return Result.success(walletService.recharge(
                    userId,
                    request.getCardId(),
                    request.getCardPassword(),
                    request.getAmount()
            ));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    private Long currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null || claims.get("id") == null) {
            return null;
        }
        return ((Number) claims.get("id")).longValue();
    }
}
