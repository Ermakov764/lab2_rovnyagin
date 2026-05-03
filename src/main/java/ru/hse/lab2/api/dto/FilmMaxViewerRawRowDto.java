package ru.hse.lab2.api.dto;

/**
 * Строка «фильм — день — число зрителей» для internal-ответов: legacy {@code film-max-viewer-rows}
 * и промежуточные ряды при сборке сводки в Additional.
 */
public record FilmMaxViewerRawRowDto(
        Long filmId,
        String filmTitle,
        /** ISO-8601 календарная дата ({@code yyyy-MM-dd}) */
        String day,
        long viewersCount
) {
}
