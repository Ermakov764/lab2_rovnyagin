package ru.hse.lab2.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hse.lab2.api.dto.FilmMaxViewerRawRowDto;
import ru.hse.lab2.exception.ValidationException;
import ru.hse.lab2.service.port.TicketStore;

import java.time.LocalDate;
import java.util.List;

@Service
public class TicketAnalyticsReadService {

    private final TicketStore ticketStore;

    public TicketAnalyticsReadService(TicketStore ticketStore) {
        this.ticketStore = ticketStore;
    }

    @Transactional(readOnly = true)
    public List<FilmMaxViewerRawRowDto> filmMaxViewerRawRows(int limit) {
        if (limit < 1 || limit > 50_000) {
            throw new ValidationException("limit must be between 1 and 50000");
        }
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
        if (raw instanceof LocalDate ld) {
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
