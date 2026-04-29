package ru.hse.lab8.additional.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MaxViewersPerDayDto {

    private Long filmId;
    private LocalDate sessionDate;
    private Long viewersCount;

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
