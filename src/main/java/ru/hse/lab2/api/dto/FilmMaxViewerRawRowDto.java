package ru.hse.lab2.api.dto;

/**
 * Ответ только для GET /api/internal/cinema/film-max-viewer-rows —
 * сырая строка агрегата из БД (без бизнес-имени «Summary» доп. сервиса).
 */
public record FilmMaxViewerRawRowDto(
        Long filmId,
        String filmTitle,
        /** ISO-8601 календарная дата ({@code yyyy-MM-dd}) */
        String day,
        long viewersCount
) {
}
