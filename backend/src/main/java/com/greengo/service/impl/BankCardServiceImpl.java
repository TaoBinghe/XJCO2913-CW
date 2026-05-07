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

import java.util.List;
import java.util.Objects;

@Service
public class BankCardServiceImpl implements BankCardService {

    @Autowired
    private BankCardMapper bankCardMapper;

    @Override
    public List<BankCardSummary> listCards(Long userId) {
        return bankCardMapper.selectList(new QueryWrapper<BankCard>()
                        .eq("user_id", userId)
                        .orderByDesc("id"))
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public BankCardSummary bindCard(Long userId, BankCardBindRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Bank card request is missing");
        }

        String bankName = normalizeText(request.getBankName());
        String holderName = normalizeText(request.getHolderName());
        String cardNumber = normalizeCardNumber(request.getCardNumber());
        String cardPassword = request.getCardPassword();

        if (bankName == null || holderName == null || cardNumber == null) {
            throw new IllegalArgumentException("Bank name, holder name, and card number are required");
        }
        if (cardPassword == null || cardPassword.isBlank()) {
            throw new IllegalArgumentException("Card password is required");
        }

        Long exists = bankCardMapper.selectCount(new QueryWrapper<BankCard>()
                .eq("user_id", userId)
                .eq("card_number", cardNumber));
        if (exists != null && exists > 0) {
            throw new IllegalArgumentException("This bank card is already bound");
        }

        BankCard bankCard = BankCard.builder()
                .userId(userId)
                .bankName(bankName)
                .holderName(holderName)
                .cardNumber(cardNumber)
                .cardLastFour(cardNumber.substring(cardNumber.length() - 4))
                .passwordHash(Md5Util.getMD5String(cardPassword))
                .build();

        if (bankCardMapper.insert(bankCard) <= 0) {
            throw new IllegalArgumentException("Failed to bind bank card");
        }
        return toSummary(bankCard);
    }

    @Override
    public BankCard getOwnedCard(Long userId, Long cardId) {
        if (cardId == null) {
            throw new IllegalArgumentException("Card id is required");
        }

        BankCard card = bankCardMapper.selectById(cardId);
        if (card == null || !Objects.equals(card.getUserId(), userId)) {
            throw new IllegalArgumentException("Bank card not found");
        }
        return card;
    }

    @Override
    public void verifyCardPassword(BankCard card, String rawPassword) {
        if (card == null) {
            throw new IllegalArgumentException("Bank card not found");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Card password is required");
        }
        if (!Md5Util.checkPassword(rawPassword, card.getPasswordHash())) {
            throw new IllegalArgumentException("Card password is incorrect");
        }
    }

    private BankCardSummary toSummary(BankCard bankCard) {
        String lastFour = bankCard.getCardLastFour();
        return BankCardSummary.builder()
                .id(bankCard.getId())
                .bankName(bankCard.getBankName())
                .holderName(bankCard.getHolderName())
                .maskedCardNumber("**** **** **** " + lastFour)
                .cardLastFour(lastFour)
                .build();
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeCardNumber(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[\\s-]", "");
        if (!normalized.matches("\\d{12,19}")) {
            return null;
        }
        return normalized;
    }
}
