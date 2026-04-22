package com.greengo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.greengo.domain.BankCard;
import com.greengo.domain.BankCardBindRequest;
import com.greengo.domain.BankCardSummary;
import com.greengo.mapper.BankCardMapper;
import com.greengo.service.BankCardService;
import com.greengo.utils.Md5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BankCardServiceImpl implements BankCardService {

    @Autowired
    private BankCardMapper bankCardMapper;

    @Override
    @Transactional
    public BankCardSummary bindCard(Long userId, BankCardBindRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User is required");
        }
        if (request == null) {
            throw new IllegalArgumentException("Card information is required");
        }

        String bankName = requireText(request.getBankName(), "Bank name is required");
        String holderName = requireText(request.getHolderName(), "Card holder name is required");
        String cardNumber = normalizeCardNumber(request.getCardNumber());
        String cardPassword = requireText(request.getCardPassword(), "Card password is required");

        Long existing = bankCardMapper.selectCount(new QueryWrapper<BankCard>()
                .eq("user_id", userId)
                .eq("card_number", cardNumber));
        if (existing != null && existing > 0) {
            throw new IllegalArgumentException("Card already bound");
        }

        LocalDateTime now = LocalDateTime.now();
        BankCard bankCard = BankCard.builder()
                .userId(userId)
                .bankName(bankName)
                .holderName(holderName)
                .cardNumber(cardNumber)
                .cardLastFour(cardNumber.substring(cardNumber.length() - 4))
                .passwordHash(Md5Util.getMD5String(cardPassword))
                .createdAt(now)
                .updatedAt(now)
                .build();

        if (bankCardMapper.insert(bankCard) <= 0) {
            throw new IllegalArgumentException("Failed to bind card");
        }
        return toSummary(bankCard);
    }

    @Override
    public List<BankCardSummary> listCards(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return bankCardMapper.selectList(new QueryWrapper<BankCard>()
                        .eq("user_id", userId)
                        .orderByDesc("created_at"))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public BankCard getOwnedCard(Long userId, Long cardId) {
        if (userId == null || cardId == null) {
            throw new IllegalArgumentException("Card id is required");
        }
        BankCard bankCard = bankCardMapper.selectById(cardId);
        if (bankCard == null || !userId.equals(bankCard.getUserId())) {
            throw new IllegalArgumentException("Card not found");
        }
        return bankCard;
    }

    @Override
    public void verifyCardPassword(BankCard bankCard, String cardPassword) {
        if (bankCard == null) {
            throw new IllegalArgumentException("Card not found");
        }
        if (cardPassword == null || cardPassword.isBlank()) {
            throw new IllegalArgumentException("Card password is required");
        }
        if (!Md5Util.checkPassword(cardPassword, bankCard.getPasswordHash())) {
            throw new IllegalArgumentException("Card password is incorrect");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeCardNumber(String cardNumber) {
        String normalized = requireText(cardNumber, "Card number is required").replaceAll("\\s+", "");
        if (!normalized.matches("\\d{12,19}")) {
            throw new IllegalArgumentException("Card number is invalid");
        }
        return normalized;
    }

    private BankCardSummary toSummary(BankCard bankCard) {
        return BankCardSummary.builder()
                .id(bankCard.getId())
                .bankName(bankCard.getBankName())
                .holderName(bankCard.getHolderName())
                .maskedCardNumber("**** **** **** " + bankCard.getCardLastFour())
                .cardLastFour(bankCard.getCardLastFour())
                .build();
    }
}
