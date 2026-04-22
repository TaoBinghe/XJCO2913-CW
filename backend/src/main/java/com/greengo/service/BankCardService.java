package com.greengo.service;

import com.greengo.domain.BankCard;
import com.greengo.domain.BankCardBindRequest;
import com.greengo.domain.BankCardSummary;

import java.util.List;

public interface BankCardService {

    BankCardSummary bindCard(Long userId, BankCardBindRequest request);

    List<BankCardSummary> listCards(Long userId);

    BankCard getOwnedCard(Long userId, Long cardId);

    void verifyCardPassword(BankCard bankCard, String cardPassword);
}
