package com.cinema.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "booked_seats")
public class BookedSeat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Column(name = "price_fixed", nullable = false)
    private BigDecimal fixedPrice;

    // Constructors
    public BookedSeat() {}

    public BookedSeat(Booking booking, Seat seat, BigDecimal fixedPrice) {
        this.booking = booking;
        this.seat = seat;
        this.fixedPrice = fixedPrice;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }

    public Seat getSeat() { return seat; }
    public void setSeat(Seat seat) { this.seat = seat; }

    public BigDecimal getFixedPrice() { return fixedPrice; }
    public void setFixedPrice(BigDecimal fixedPrice) { this.fixedPrice = fixedPrice; }
}