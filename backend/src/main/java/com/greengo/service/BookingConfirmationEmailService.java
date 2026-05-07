package com.greengo.service;

import com.greengo.domain.Booking;

public interface BookingConfirmationEmailService {

    void sendBookingConfirmation(Booking booking);
}
