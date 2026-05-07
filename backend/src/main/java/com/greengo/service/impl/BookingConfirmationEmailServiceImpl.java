package com.greengo.service.impl;

import com.greengo.domain.Booking;
import com.greengo.domain.User;
import com.greengo.mapper.UserMapper;
import com.greengo.service.BookingConfirmationEmailService;
import com.greengo.utils.RentalConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class BookingConfirmationEmailServiceImpl implements BookingConfirmationEmailService {

    private static final Logger log = LoggerFactory.getLogger(BookingConfirmationEmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserMapper userMapper;

    @Value("${spring.mail.from:}")
    private String mailFrom;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Override
    public void sendBookingConfirmation(Booking booking) {
        if (booking == null || booking.getId() == null || booking.getUserId() == null) {
            log.warn("Skip booking confirmation email because booking details are incomplete");
            return;
        }

        Runnable sendTask = () -> sendNow(booking);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendTask.run();
                }
            });
            return;
        }
        sendTask.run();
    }

    private void sendNow(Booking booking) {
        try {
            User user = userMapper.selectById(booking.getUserId());
            String recipientEmail = resolveRecipientEmail(user, booking);
            if (user == null || recipientEmail == null || recipientEmail.isBlank()) {
                log.warn("Skip booking confirmation email for booking {} because user {} has no email",
                        booking.getId(), booking.getUserId());
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(resolveFromAddress());
            message.setTo(recipientEmail);
            message.setSubject("Green Go booking confirmation #" + booking.getId());
            message.setText(buildMessageBody(user, booking));
            mailSender.send(message);
            log.info("Booking confirmation email sent for booking {} to {}", booking.getId(), recipientEmail);
        } catch (Exception e) {
            log.warn("Failed to send booking confirmation email for booking {}", booking.getId(), e);
        }
    }

    private String buildMessageBody(User user, Booking booking) {
        StringBuilder body = new StringBuilder();
        body.append("Hello ").append(valueOrDash(resolveCustomerDisplayName(user, booking))).append(",\n\n");
        body.append("Your Green Go booking is confirmed.\n\n");
        body.append("Order ID: #").append(booking.getId()).append("\n");
        body.append("Rental type: ").append(formatRentalType(booking.getRentalType())).append("\n");
        body.append("Status: ").append(valueOrDash(booking.getStatus())).append("\n");
        body.append("Store/Scooter: ").append(resolveBookingTarget(booking)).append("\n");
        body.append("Location: ").append(resolveBookingLocation(booking)).append("\n");
        body.append("Hire period: ").append(valueOrDash(booking.getHirePeriod())).append("\n");
        body.append("Start time: ").append(formatTime(booking.getStartTime())).append("\n");
        if (booking.getEndTime() != null) {
            body.append("End time: ").append(formatTime(booking.getEndTime())).append("\n");
        }
        if (booking.getPickupDeadline() != null) {
            body.append("Pickup deadline: ").append(formatTime(booking.getPickupDeadline())).append("\n");
        }
        body.append("Total cost: ").append(formatMoney(booking.getTotalCost())).append("\n\n");
        body.append("Thank you for using Green Go.");
        return body.toString();
    }

    private String resolveRecipientEmail(User user, Booking booking) {
        if (booking.getCustomerEmail() != null && !booking.getCustomerEmail().isBlank()) {
            return booking.getCustomerEmail().trim();
        }
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return null;
        }
        return user.getEmail().trim();
    }

    private String resolveCustomerDisplayName(User user, Booking booking) {
        if (booking.getCustomerName() != null && !booking.getCustomerName().isBlank()) {
            return booking.getCustomerName().trim();
        }
        return user == null ? null : user.getUsername();
    }

    private String resolveFromAddress() {
        if (mailFrom != null && !mailFrom.isBlank()) {
            return mailFrom.trim();
        }
        if (mailUsername != null && !mailUsername.isBlank()) {
            return mailUsername.trim();
        }
        return "no-reply@green-go.local";
    }

    private String formatRentalType(String rentalType) {
        if (RentalConstants.RENTAL_TYPE_STORE_PICKUP.equals(rentalType)) {
            return "Store Pickup";
        }
        if (RentalConstants.RENTAL_TYPE_SCAN_RIDE.equals(rentalType)) {
            return "Scan Ride";
        }
        return valueOrDash(rentalType);
    }

    private String resolveBookingTarget(Booking booking) {
        if (RentalConstants.RENTAL_TYPE_STORE_PICKUP.equals(booking.getRentalType())) {
            return valueOrDash(booking.getStoreName());
        }
        return valueOrDash(booking.getScooterCode());
    }

    private String resolveBookingLocation(Booking booking) {
        if (RentalConstants.RENTAL_TYPE_STORE_PICKUP.equals(booking.getRentalType())) {
            return valueOrDash(booking.getStoreAddress());
        }
        return valueOrDash(booking.getPickupLocation());
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "-" : value.toString();
    }

    private String formatMoney(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        return "GBP " + amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
