package com.cinema.service;

import com.cinema.entity.Booking;
import com.cinema.entity.BookedSeat;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    @Value("${app.notification.booking-subject}")
    private String bookingSubject;


    // Асинхронная отправка подтверждения брони
    @Async
    public void sendBookingConfirmation(String toEmail, Booking booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(bookingSubject);

            // Формируем HTML-тело письма
            String htmlContent = buildBookingHtml(booking);
            helper.setText(htmlContent, true); // true = это HTML

            mailSender.send(message);
            log.info("Уведомление о брони #{} успешно отправлено на {}", booking.getId(), toEmail);

        } catch (MessagingException e) {
            // Логируем ошибку, но НЕ выбрасываем исключение,
            // чтобы не ломать транзакцию создания брони
            log.error("Ошибка отправки email для брони #{}: {}", booking.getId(), e.getMessage());
        }
    }

    private String buildBookingHtml(Booking booking) {
        StringBuilder seatsList = new StringBuilder();
        for (BookedSeat bs : booking.getBookedSeats()) {
            seatsList.append(String.format("<li>Ряд %d, Место %d (%.2f ₽)</li>",
                    bs.getSeat().getRowNumber(),
                    bs.getSeat().getSeatNumber(),
                    bs.getFixedPrice()));
        }

        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <h2 style="color: #e50914;">Бронь подтверждена!</h2>
                <p>Ваша бронь успешно создана!</p>
                
                <div style="background: #f5f5f5; padding: 15px; border-radius: 8px; margin: 20px 0;">
                    <p><strong>Фильм:</strong> %s</p>
                    <p><strong>Зал:</strong> %s</p>
                    <p><strong>Дата и время:</strong> %s</p>
                    <p><strong>Номер брони:</strong> #%d</p>
                </div>
                
                <h3>Выбранные места:</h3>
                <ul>%s</ul>
                
                <p style="font-size: 18px;"><strong>Сумма: %.2f ₽</strong></p>
                
                <p style="color: #666; font-size: 12px; margin-top: 30px;">
                    Пожалуйста, предъявите этот номер брони на кассе или используйте QR-код в личном кабинете.
                </p>
            </div>
            """.formatted(
                booking.getSession().getMovie().getTitle(),
                booking.getSession().getHall().getName(),
                booking.getSession().getStartTime(),
                booking.getId(),
                seatsList,
                booking.getTotalAmount()
        );
    }
}