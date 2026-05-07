package com.greengo.impl;

import com.greengo.domain.Booking;
import com.greengo.domain.User;
import com.greengo.mapper.UserMapper;
import com.greengo.service.impl.BookingConfirmationEmailServiceImpl;
import com.greengo.utils.RentalConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingConfirmationEmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserMapper userMapper;

    private BookingConfirmationEmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new BookingConfirmationEmailServiceImpl();
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);
        ReflectionTestUtils.setField(emailService, "userMapper", userMapper);
        ReflectionTestUtils.setField(emailService, "mailFrom", "green-go@example.com");
    }

    @Test
    void sendBookingConfirmationBuildsExpectedMessage() {
        Booking booking = Booking.builder()
                .id(10L)
                .userId(1L)
                .rentalType(RentalConstants.RENTAL_TYPE_STORE_PICKUP)
                .status(RentalConstants.BOOKING_STATUS_RESERVED)
                .storeName("Xipu North Hub")
                .storeAddress("Xipu Campus Library North Plaza")
                .hirePeriod("DAY_1")
                .startTime(LocalDateTime.of(2026, 4, 16, 10, 0))
                .endTime(LocalDateTime.of(2026, 4, 17, 10, 0))
                .pickupDeadline(LocalDateTime.of(2026, 4, 16, 10, 30))
                .totalCost(new BigDecimal("30.00"))
                .build();
        when(userMapper.selectById(1L)).thenReturn(User.builder()
                .username("alice01")
                .email("alice@example.com")
                .build());

        emailService.sendBookingConfirmation(booking);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("green-go@example.com", message.getFrom());
        assertEquals("Green Go booking confirmation #10", message.getSubject());
        assertEquals("alice@example.com", message.getTo()[0]);
        assertTrue(message.getText().contains("Order ID: #10"));
        assertTrue(message.getText().contains("Rental type: Store Pickup"));
        assertTrue(message.getText().contains("Total cost: GBP 30.00"));
    }

    @Test
    void sendBookingConfirmationSkipsUsersWithoutEmail() {
        when(userMapper.selectById(1L)).thenReturn(User.builder()
                .username("alice01")
                .build());

        emailService.sendBookingConfirmation(Booking.builder()
                .id(10L)
                .userId(1L)
                .build());

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
