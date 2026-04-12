package ru.hse.lab2.api.request;

import java.time.LocalDate;
import java.time.LocalTime;

public class TicketRequest {

    private Long viewerId;
    private Long filmId;
    private LocalDate sessionDate;
    private LocalTime sessionTime;
    private String seatNumber;
    private Double price;

    public TicketRequest() {
    }

    public TicketRequest(
            Long viewerId,
            Long filmId,
            LocalDate sessionDate,
            LocalTime sessionTime,
            String seatNumber,
            Double price
    ) {
        this.viewerId = viewerId;
        this.filmId = filmId;
        this.sessionDate = sessionDate;
        this.sessionTime = sessionTime;
        this.seatNumber = seatNumber;
        this.price = price;
    }

    public Long getViewerId() {
        return viewerId;
    }

    public void setViewerId(Long viewerId) {
        this.viewerId = viewerId;
    }

    public Long getFilmId() {
        return filmId;
    }

    public void setFilmId(Long filmId) {
        this.filmId = filmId;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
    }

    public LocalTime getSessionTime() {
        return sessionTime;
    }

    public void setSessionTime(LocalTime sessionTime) {
        this.sessionTime = sessionTime;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }
}
