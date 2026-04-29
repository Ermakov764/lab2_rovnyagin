package ru.hse.lab2.api.dto;

import java.time.LocalDate;

/**
 * По каждому фильму с билетами: календарный день с максимальной «загрузкой»
 * (та же метрика, что и {@link MaxViewersPerDayDto} для одного filmId).
 */
public record FilmMaxViewersSummaryDto(
        Long filmId,
        String filmTitle,
        LocalDate day,
        long maxViewersOnSessionForDay
) {
}
