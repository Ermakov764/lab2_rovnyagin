package ru.hse.lab2.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hse.lab2.api.dto.FilmMaxViewerRawRowDto;
import ru.hse.lab.shared.analytics.TopFilmsReportLimits;
import ru.hse.lab2.exception.ValidationException;
import ru.hse.lab2.service.port.TicketStore;

import java.util.List;

/**
 * Данные для <b>internal</b> {@code GET /api/internal/cinema/film-max-viewer-rows} (агрегат уже в SQL).
 *
 * <p>Микросервис Additional в лаб. 8 этот сервис <b>не использует</b>: там только публичные
 * {@code /api/films} и {@code /api/tickets?filmId=}.
 */
@Service
public class TicketAnalyticsReadService {

    private final TicketStore ticketStore;

    public TicketAnalyticsReadService(TicketStore ticketStore) {
        this.ticketStore = ticketStore;
    }

    /** Internal legacy: агрегат с «лучшим днём» уже в репозитории ({@code findAllFilmDailyViewerAggregates}). */
    @Transactional(readOnly = true)
    public List<FilmMaxViewerRawRowDto> filmMaxViewerRawRows(int limit) {
        TopFilmsReportLimits.requireInRange(limit);
        return ticketStore.findAllFilmDailyViewerAggregates(limit).stream()
                .map(row -> new FilmMaxViewerRawRowDto(
                        ((Number) row[0]).longValue(),
                        row[1] != null ? String.valueOf(row[1]) : null,
                        dayIso(row[2]),
                        ((Number) row[3]).longValue()
                ))
                .toList();
    }

    private static String dayIso(Object raw) {
        if (raw == null) {
            throw new ValidationException("Analytics row has null date");
        }
        if (raw instanceof java.time.LocalDate ld) {
            return ld.toString();
        }
        if (raw instanceof java.sql.Date d) {
            return d.toLocalDate().toString();
        }
        if (raw instanceof java.util.Date ud) {
            return new java.sql.Date(ud.getTime()).toLocalDate().toString();
        }
        throw new ValidationException("Unsupported date type in analytics: " + raw.getClass());
    }
}
