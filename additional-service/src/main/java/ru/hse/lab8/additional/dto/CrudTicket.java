package ru.hse.lab8.additional.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;

/**
 * Минимальный снимок билета из {@code GET /api/tickets}: только join и агрегация по уникальным зрителям в день.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CrudTicket(
        Long filmId,
        Long viewerId,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate sessionDate
) {
}
