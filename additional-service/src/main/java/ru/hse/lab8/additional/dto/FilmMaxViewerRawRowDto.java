package ru.hse.lab8.additional.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Сырой ответ основного сервиса GET /api/internal/cinema/film-max-viewer-rows.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FilmMaxViewerRawRowDto {

    private Long filmId;
    private String filmTitle;
    private String day;
    private long viewersCount;

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

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public long getViewersCount() {
        return viewersCount;
    }

    public void setViewersCount(long viewersCount) {
        this.viewersCount = viewersCount;
    }
}
