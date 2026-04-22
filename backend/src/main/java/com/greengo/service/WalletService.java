package com.greengo.service;

import com.greengo.domain.WalletSummary;

import java.math.BigDecimal;

public interface WalletService {

    WalletSummary getSummary(Long userId);

    WalletSummary recharge(Long userId, Long cardId, String cardPassword, BigDecimal amount);

    void payBooking(Long userId, Long bookingId, BigDecimal amount);
}
