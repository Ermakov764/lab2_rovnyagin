package ru.hse.lab2.api.dto;

import java.time.LocalDate;

public class MaxViewersPerDayDto {

    private Long filmId;
    private LocalDate sessionDate;
    private Long viewersCount;

    public MaxViewersPerDayDto() {
    }

    public MaxViewersPerDayDto(Long filmId, LocalDate sessionDate, Long viewersCount) {
        this.filmId = filmId;
        this.sessionDate = sessionDate;
        this.viewersCount = viewersCount;
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

    public Long getViewersCount() {
        return viewersCount;
    }

    public void setViewersCount(Long viewersCount) {
        this.viewersCount = viewersCount;
    }
}
