package com.greengo.service.impl;

import com.greengo.domain.BankCard;
import com.greengo.domain.User;
import com.greengo.domain.WalletSummary;
import com.greengo.domain.WalletTransaction;
import com.greengo.mapper.UserMapper;
import com.greengo.mapper.WalletTransactionMapper;
import com.greengo.service.BankCardService;
import com.greengo.service.WalletService;
import com.greengo.utils.RentalConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class WalletServiceImpl implements WalletService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BankCardService bankCardService;

    @Autowired
    private WalletTransactionMapper walletTransactionMapper;

    @Override
    public WalletSummary getSummary(Long userId) {
        User user = requireUser(userId);
        return WalletSummary.builder()
                .balance(safeBalance(user))
                .cards(bankCardService.listCards(userId))
                .build();
    }

    @Override
    @Transactional
    public WalletSummary recharge(Long userId, Long cardId, String cardPassword, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Recharge amount must be greater than zero");
        }

        BankCard bankCard = bankCardService.getOwnedCard(userId, cardId);
        bankCardService.verifyCardPassword(bankCard, cardPassword);

        requireUser(userId);
        if (userMapper.incrementWalletBalance(userId, amount) <= 0) {
            throw new IllegalArgumentException("Failed to update wallet balance");
        }

        BigDecimal newBalance = safeBalance(requireUser(userId));
        recordTransaction(userId, RentalConstants.WALLET_TRANSACTION_TYPE_RECHARGE, amount, newBalance, null, bankCard.getId());
        return WalletSummary.builder()
                .balance(newBalance)
                .cards(bankCardService.listCards(userId))
                .build();
    }

    @Override
    @Transactional
    public void payBooking(Long userId, Long bookingId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Payment amount is invalid");
        }

        requireUser(userId);
        if (userMapper.decrementWalletBalanceIfEnough(userId, amount) <= 0) {
            throw new IllegalArgumentException("Insufficient wallet balance");
        }

        BigDecimal newBalance = safeBalance(requireUser(userId));
        recordTransaction(userId, RentalConstants.WALLET_TRANSACTION_TYPE_BOOKING_PAYMENT, amount, newBalance, bookingId, null);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return user;
    }

    private BigDecimal safeBalance(User user) {
        return user.getWalletBalance() == null ? BigDecimal.ZERO : user.getWalletBalance();
    }

    private void recordTransaction(Long userId,
                                   String type,
                                   BigDecimal amount,
                                   BigDecimal balanceAfter,
                                   Long bookingId,
                                   Long bankCardId) {
        WalletTransaction transaction = WalletTransaction.builder()
                .userId(userId)
                .type(type)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .bookingId(bookingId)
                .bankCardId(bankCardId)
                .build();
        if (walletTransactionMapper.insert(transaction) <= 0) {
            throw new IllegalArgumentException("Failed to record wallet transaction");
        }
    }
}
