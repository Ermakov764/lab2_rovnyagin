package ru.hse.lab8.additional.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

/**
 * Результат аналитики по одному фильму: «лучший» день по числу уникальных зрителей и это число.
 * Используется как элемент списка сводки {@code GET /api/analytics/films/max-viewers-summary}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FilmStats(
        String filmTitle,
        Long filmId,
        LocalDate day,
        long maxViewersOnSessionForDay
) {
}
