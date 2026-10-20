package com.cinema.entity;

public enum BookingStatus {
    PENDING,      // Ожидает оплаты
    CONFIRMED,    // Подтверждено
    CANCELLED,    // Отменено
    EXPIRED       // Истекло
}