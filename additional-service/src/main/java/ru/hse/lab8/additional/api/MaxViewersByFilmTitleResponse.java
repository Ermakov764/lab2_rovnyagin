package ru.hse.lab8.additional.api;

import java.time.LocalDate;

/**
 * Выход ТЗ лаб. 8 (дополнительно): день и максимум зрителей на сеансе в этот день;
 * join по названию фильма выполнен в Additional service (не в БД).
 */
public record MaxViewersByFilmTitleResponse(
        String filmTitle,
        Long filmId,
        LocalDate day,
        long maxViewersOnSessionForDay
) {
}
