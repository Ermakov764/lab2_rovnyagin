package ru.hse.lab8.additional.service;

import org.springframework.stereotype.Service;
import ru.hse.lab8.additional.client.MainCrudTicketClient;
import ru.hse.lab8.additional.dto.FilmMaxViewerRawRowDto;
import ru.hse.lab8.additional.dto.FilmMaxViewersSummaryDto;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Логика доп. сервиса: HTTP к CRUD через {@link MainCrudTicketClient}, приведение сырых строк к полям Summary.
 */
@Service
public class FilmViewersSummaryService {

    private final MainCrudTicketClient ticketClient;

    public FilmViewersSummaryService(MainCrudTicketClient ticketClient) {
        this.ticketClient = ticketClient;
    }

    public List<FilmMaxViewersSummaryDto> filmMaxViewersSummary(int limit) {
        if (limit < 1 || limit > 50_000) {
            throw new IllegalArgumentException("limit must be between 1 and 50000");
        }
        return ticketClient.fetchFilmMaxViewerRawRows(limit).stream()
                .map(FilmViewersSummaryService::toSummaryDto)
                .toList();
    }

    private static FilmMaxViewersSummaryDto toSummaryDto(FilmMaxViewerRawRowDto raw) {
        FilmMaxViewersSummaryDto d = new FilmMaxViewersSummaryDto();
        d.setFilmId(raw.getFilmId());
        d.setFilmTitle(raw.getFilmTitle());
        try {
            d.setDay(LocalDate.parse(raw.getDay()));
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalStateException("Invalid day ISO in CRUD row: " + raw.getDay(), e);
        }
        d.setMaxViewersOnSessionForDay(raw.getViewersCount());
        return d;
    }
}
