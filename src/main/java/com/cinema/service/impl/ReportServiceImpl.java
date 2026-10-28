package com.cinema.service.impl;

import com.cinema.entity.Booking;
import com.cinema.repository.BookingRepository;
import com.cinema.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final BookingRepository bookingRepository;

    @Override
    public byte[] generateSalesReportExcel(LocalDate startDate, LocalDate endDate) {
        // 1. Получаем данные из БД
        List<Booking> bookings = bookingRepository.findConfirmedBookingsByDateRange(
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
        );

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Отчет по продажам");

            // 2. Создаем заголовки
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID Брони", "Дата", "Пользователь", "Фильм", "Зал", "Места", "Сумма"};

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 3. Заполняем данными
            int rowNum = 1;
            BigDecimal totalSum = BigDecimal.ZERO;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

            for (Booking b : bookings) {
                Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(b.getId());
                row.createCell(1).setCellValue(b.getCreatedAt().format(formatter));
                row.createCell(2).setCellValue(b.getUser().getEmail());
                row.createCell(3).setCellValue(b.getSession().getMovie().getTitle());
                row.createCell(4).setCellValue(b.getSession().getHall().getName());

                // Собираем места в одну строку
                String seats = b.getBookedSeats().stream()
                        .map(bs -> bs.getSeat().getRowNumber() + "-" + bs.getSeat().getSeatNumber())
                        .reduce((a, bStr) -> a + ", " + bStr)
                        .orElse("");
                row.createCell(5).setCellValue(seats);

                row.createCell(6).setCellValue(b.getTotalAmount().doubleValue());

                totalSum = totalSum.add(b.getTotalAmount());
            }

            // 4. Итоговая строка
            Row totalRow = sheet.createRow(rowNum + 1);
            totalRow.createCell(5).setCellValue("ИТОГО:");
            totalRow.createCell(6).setCellValue(totalSum.doubleValue());

            // Автоширина колонок
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(bos);
            return bos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Ошибка генерации Excel отчета", e);
        }
    }
}