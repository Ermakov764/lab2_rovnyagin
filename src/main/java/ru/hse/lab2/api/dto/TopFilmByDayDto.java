package ru.hse.lab2.api.dto;

import java.time.LocalDate;

public class TopFilmByDayDto {
    private Long filmId;
    private String title;
    private LocalDate sessionDate;
    private Long viewersCount;

    public TopFilmByDayDto() {
    }

    public TopFilmByDayDto(Long filmId, String title, LocalDate sessionDate, Long viewersCount) {
        this.filmId = filmId;
        this.title = title;
        this.sessionDate = sessionDate;
        this.viewersCount = viewersCount;
    }

    public Long getFilmId() {
        return filmId;
    }

    public void setFilmId(Long filmId) {
        this.filmId = filmId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
