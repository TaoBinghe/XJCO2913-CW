package com.greengo.service;

import com.greengo.domain.BankCard;
import com.greengo.domain.BankCardBindRequest;
import com.greengo.domain.BankCardSummary;

import java.util.List;

public interface BankCardService {

    List<BankCardSummary> listCards(Long userId);

    BankCardSummary bindCard(Long userId, BankCardBindRequest request);

    BankCard getOwnedCard(Long userId, Long cardId);

    void verifyCardPassword(BankCard card, String rawPassword);
}
