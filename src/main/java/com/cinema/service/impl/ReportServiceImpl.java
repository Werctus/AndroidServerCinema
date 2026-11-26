package com.cinema.service.impl;

import com.cinema.entity.Booking;
import com.cinema.repository.BookingRepository;
import com.cinema.service.ReportService;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
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

    @Override
    public byte[] generateSalesReportPdf(LocalDate startDate, LocalDate endDate) {
        List<Booking> bookings = bookingRepository.findConfirmedBookingsByDateRange(
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
        );

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, bos);
            document.open();

            com.lowagie.text.Font titleFont = createPdfFont(16, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font headerFont = createPdfFont(10, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font cellFont = createPdfFont(9, com.lowagie.text.Font.NORMAL);

            Paragraph title = new Paragraph(
                    "Отчет по продажам билетов за период: " + startDate + " — " + endDate,
                    titleFont
            );
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(15);
            document.add(title);

            PdfPTable table = new PdfPTable(new float[]{1.2f, 2.5f, 3.2f, 3.0f, 2.0f, 2.5f, 1.8f});
            table.setWidthPercentage(100);

            String[] headers = {"ID", "Дата", "Пользователь", "Фильм", "Зал", "Места", "Сумма (₽)"};
            for (String h : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(h, headerFont));
                headerCell.setBackgroundColor(new Color(230, 230, 230));
                headerCell.setPadding(6);
                table.addCell(headerCell);
            }

            BigDecimal totalSum = BigDecimal.ZERO;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

            for (Booking b : bookings) {
                table.addCell(new Phrase(String.valueOf(b.getId()), cellFont));
                table.addCell(new Phrase(b.getCreatedAt().format(formatter), cellFont));
                table.addCell(new Phrase(b.getUser().getEmail(), cellFont));
                table.addCell(new Phrase(b.getSession().getMovie().getTitle(), cellFont));
                table.addCell(new Phrase(b.getSession().getHall().getName(), cellFont));

                String seats = b.getBookedSeats().stream()
                        .map(bs -> bs.getSeat().getRowNumber() + "-" + bs.getSeat().getSeatNumber())
                        .reduce((a, bStr) -> a + ", " + bStr)
                        .orElse("");
                table.addCell(new Phrase(seats, cellFont));
                table.addCell(new Phrase(b.getTotalAmount().toPlainString(), cellFont));

                totalSum = totalSum.add(b.getTotalAmount());
            }

            document.add(table);

            Paragraph totalParagraph = new Paragraph(
                    "ИТОГО: " + totalSum.toPlainString() + " ₽ (Всего заказов: " + bookings.size() + ")",
                    titleFont
            );
            totalParagraph.setAlignment(Element.ALIGN_RIGHT);
            totalParagraph.setSpacingBefore(15);
            document.add(totalParagraph);

            document.close();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации PDF отчета", e);
        }
    }

    private com.lowagie.text.Font createPdfFont(float size, int style) {
        String winFontPath = "C:/Windows/Fonts/arial.ttf";
        if (new File(winFontPath).exists()) {
            try {
                BaseFont baseFont = BaseFont.createFont(winFontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                return new com.lowagie.text.Font(baseFont, size, style);
            } catch (Exception ignored) {
            }
        }
        return FontFactory.getFont(FontFactory.HELVETICA, size, style);
    }
}