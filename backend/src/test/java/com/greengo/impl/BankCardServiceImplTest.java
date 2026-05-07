package com.greengo.impl;

import com.greengo.domain.BankCard;
import com.greengo.domain.BankCardBindRequest;
import com.greengo.domain.BankCardSummary;
import com.greengo.mapper.BankCardMapper;
import com.greengo.service.impl.BankCardServiceImpl;
import com.greengo.utils.CardFingerprintUtil;
import com.greengo.utils.Md5Util;
import com.greengo.utils.PasswordHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankCardServiceImplTest {

    private static final Long USER_ID = 1L;

    @Mock
    private BankCardMapper bankCardMapper;

    private BankCardServiceImpl bankCardService;

    @BeforeEach
    void setUp() {
        bankCardService = new BankCardServiceImpl();
        ReflectionTestUtils.setField(bankCardService, "bankCardMapper", bankCardMapper);
    }

    @Test
    void bindCardStoresFingerprintAndBcryptHashWithoutFullCardNumber() {
        BankCardBindRequest request = bindRequest();
        when(bankCardMapper.selectCount(any())).thenReturn(0L);
        when(bankCardMapper.insert(any(BankCard.class))).thenAnswer(invocation -> {
            BankCard bankCard = invocation.getArgument(0);
            bankCard.setId(99L);
            return 1;
        });

        BankCardSummary summary = bankCardService.bindCard(USER_ID, request);

        ArgumentCaptor<BankCard> cardCaptor = ArgumentCaptor.forClass(BankCard.class);
        verify(bankCardMapper).insert(cardCaptor.capture());
        BankCard stored = cardCaptor.getValue();
        assertNull(stored.getCardNumber());
        assertEquals("1234", stored.getCardLastFour());
        assertEquals(CardFingerprintUtil.fingerprint("4111111111111234"), stored.getCardFingerprint());
        assertNotEquals(Md5Util.getMD5String("123456"), stored.getPasswordHash());
        assertTrue(PasswordHashUtil.matches("123456", stored.getPasswordHash()));
        assertEquals("**** **** **** 1234", summary.getMaskedCardNumber());
    }

    @Test
    void bindCardRejectsDuplicateByFingerprintOrLegacyCardNumber() {
        BankCardBindRequest request = bindRequest();
        when(bankCardMapper.selectCount(any())).thenReturn(1L);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bankCardService.bindCard(USER_ID, request));

        assertEquals("This bank card is already bound", error.getMessage());
        verify(bankCardMapper, never()).insert(any(BankCard.class));
    }

    @Test
    void verifyCardPasswordAcceptsBcryptHashWithoutUpdatingCard() {
        BankCard card = BankCard.builder()
                .id(99L)
                .userId(USER_ID)
                .cardLastFour("1234")
                .cardFingerprint(CardFingerprintUtil.fingerprint("4111111111111234"))
                .passwordHash(PasswordHashUtil.hash("123456"))
                .build();

        bankCardService.verifyCardPassword(card, "123456");

        verify(bankCardMapper, never()).upgradeSecurity(any(BankCard.class));
    }

    @Test
    void verifyCardPasswordRejectsWrongPassword() {
        BankCard card = BankCard.builder()
                .id(99L)
                .userId(USER_ID)
                .passwordHash(PasswordHashUtil.hash("123456"))
                .build();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> bankCardService.verifyCardPassword(card, "000000"));

        assertEquals("Card password is incorrect", error.getMessage());
        verify(bankCardMapper, never()).upgradeSecurity(any(BankCard.class));
    }

    @Test
    void verifyCardPasswordUpgradesLegacyMd5HashAndClearsFullCardNumber() {
        String legacyHash = Md5Util.getMD5String("123456");
        BankCard card = BankCard.builder()
                .id(99L)
                .userId(USER_ID)
                .cardNumber("4111 1111 1111 1234")
                .cardLastFour("1234")
                .passwordHash(legacyHash)
                .build();
        when(bankCardMapper.upgradeSecurity(card)).thenReturn(1);

        bankCardService.verifyCardPassword(card, "123456");

        assertNull(card.getCardNumber());
        assertEquals(CardFingerprintUtil.fingerprint("4111111111111234"), card.getCardFingerprint());
        assertNotEquals(legacyHash, card.getPasswordHash());
        assertTrue(PasswordHashUtil.matches("123456", card.getPasswordHash()));
        verify(bankCardMapper).upgradeSecurity(card);
    }

    private BankCardBindRequest bindRequest() {
        BankCardBindRequest request = new BankCardBindRequest();
        request.setBankName("Green Bank");
        request.setHolderName("Alice");
        request.setCardNumber("4111 1111 1111 1234");
        request.setCardPassword("123456");
        return request;
    }
}
