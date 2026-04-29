package ru.hse.lab8.additional.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

/**
 * Итог доп. сервиса для клиента: после приведения сырых строк CRUD к полям ниже.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FilmMaxViewersSummaryDto {

    private Long filmId;
    private String filmTitle;
    private LocalDate day;
    private long maxViewersOnSessionForDay;

    public Long getFilmId() {
        return filmId;
    }

    public void setFilmId(Long filmId) {
        this.filmId = filmId;
    }

    public String getFilmTitle() {
        return filmTitle;
    }

    public void setFilmTitle(String filmTitle) {
        this.filmTitle = filmTitle;
    }

    public LocalDate getDay() {
        return day;
    }

    public void setDay(LocalDate day) {
        this.day = day;
    }

    public long getMaxViewersOnSessionForDay() {
        return maxViewersOnSessionForDay;
    }

    public void setMaxViewersOnSessionForDay(long maxViewersOnSessionForDay) {
        this.maxViewersOnSessionForDay = maxViewersOnSessionForDay;
    }
}
