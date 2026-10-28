package com.cinema.service;

import java.time.LocalDate;

public interface ReportService {
    byte[] generateSalesReportExcel(LocalDate startDate, LocalDate endDate);
}