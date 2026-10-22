package com.cinema.service;

import com.cinema.entity.Booking;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {
    public void sendBookingConfirmation(String email, Booking booking) {
        // TODO: Реальная отправка email через JavaMailSender
        System.out.println(">>> MOCK EMAIL SENT to: " + email +
                " for booking #" + booking.getId());
    }
}