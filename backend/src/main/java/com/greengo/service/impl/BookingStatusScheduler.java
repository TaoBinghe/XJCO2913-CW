package com.greengo.service.impl;

import com.greengo.service.BookingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingStatusScheduler {

    @Autowired
    private BookingService bookingService;

    @Scheduled(fixedDelay = 60000)
    public void advanceStoreRentalStatuses() {
        bookingService.expireReservations();
        bookingService.markOverdueBookings();
    }
}
