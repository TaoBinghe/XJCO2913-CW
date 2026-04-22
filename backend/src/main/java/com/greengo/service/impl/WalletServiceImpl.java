package com.greengo.service.impl;

import com.greengo.domain.BankCard;
import com.greengo.domain.User;
import com.greengo.domain.WalletSummary;
import com.greengo.domain.WalletTransaction;
import com.greengo.mapper.UserMapper;
import com.greengo.mapper.WalletTransactionMapper;
import com.greengo.service.BankCardService;
import com.greengo.service.DistributedLockService;
import com.greengo.service.WalletService;
import com.greengo.utils.LockKeys;
import com.greengo.utils.RentalConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class WalletServiceImpl implements WalletService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BankCardService bankCardService;

    @Autowired
    private WalletTransactionMapper walletTransactionMapper;

    @Autowired(required = false)
    private DistributedLockService distributedLockService = new LocalDistributedLockService();

    @Override
    public WalletSummary getSummary(Long userId) {
        User user = requireUser(userId);
        return buildSummary(user);
    }

    @Override
    @Transactional
    public WalletSummary recharge(Long userId, Long cardId, String cardPassword, BigDecimal amount) {
        BigDecimal rechargeAmount = requirePositiveAmount(amount);
        return distributedLockService.executeWithLock(LockKeys.userWalletLock(userId), () -> {
            BankCard bankCard = bankCardService.getOwnedCard(userId, cardId);
            bankCardService.verifyCardPassword(bankCard, cardPassword);

            User user = requireUser(userId);
            BigDecimal balanceAfter = walletBalance(user).add(rechargeAmount);
            user.setWalletBalance(balanceAfter);
            if (userMapper.updateById(user) <= 0) {
                throw new IllegalArgumentException("Failed to update wallet balance");
            }

            insertTransaction(
                    userId,
                    RentalConstants.WALLET_TRANSACTION_TYPE_RECHARGE,
                    rechargeAmount,
                    balanceAfter,
                    null,
                    cardId
            );
            return buildSummary(user);
        });
    }

    @Override
    @Transactional
    public void payBooking(Long userId, Long bookingId, BigDecimal amount) {
        BigDecimal paymentAmount = requireNonNegativeAmount(amount);
        distributedLockService.executeWithLock(LockKeys.userWalletLock(userId), () -> {
            User user = requireUser(userId);
            BigDecimal currentBalance = walletBalance(user);
            if (currentBalance.compareTo(paymentAmount) < 0) {
                throw new IllegalArgumentException("Insufficient wallet balance");
            }

            BigDecimal balanceAfter = currentBalance.subtract(paymentAmount);
            user.setWalletBalance(balanceAfter);
            if (userMapper.updateById(user) <= 0) {
                throw new IllegalArgumentException("Failed to update wallet balance");
            }

            insertTransaction(
                    userId,
                    RentalConstants.WALLET_TRANSACTION_TYPE_BOOKING_PAYMENT,
                    paymentAmount,
                    balanceAfter,
                    bookingId,
                    null
            );
        });
    }

    private User requireUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User is required");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return user;
    }

    private BigDecimal requirePositiveAmount(BigDecimal amount) {
        BigDecimal normalized = requireNonNegativeAmount(amount);
        if (normalized.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        return normalized;
    }

    private BigDecimal requireNonNegativeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        return normalized;
    }

    private BigDecimal walletBalance(User user) {
        if (user.getWalletBalance() == null) {
            return ZERO;
        }
        return user.getWalletBalance().setScale(2, RoundingMode.HALF_UP);
    }

    private void insertTransaction(Long userId,
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
                .createdAt(LocalDateTime.now())
                .build();
        if (walletTransactionMapper.insert(transaction) <= 0) {
            throw new IllegalArgumentException("Failed to record wallet transaction");
        }
    }

    private WalletSummary buildSummary(User user) {
        return WalletSummary.builder()
                .balance(walletBalance(user))
                .cards(bankCardService.listCards(user.getId()))
                .build();
    }
}
