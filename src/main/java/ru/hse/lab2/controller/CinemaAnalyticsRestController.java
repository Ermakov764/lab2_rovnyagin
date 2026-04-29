package ru.hse.lab2.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.lab2.api.dto.FilmMaxViewersSummaryDto;
import ru.hse.lab2.service.TicketService;

import java.util.List;

/**
 * Сводка по всем фильмам с билетами.
 * <p>
 * Путь вынесен из {@code TicketRestController}: у {@code GET /api/tickets/{id}} и
 * {@code GET /api/tickets/analytics/...} в ряде конфигураций Spring порядок сопоставления
 * мог приводить к тому, что запрос обрабатывался не тем хендлером (и соединение рвалось).
 */
@RestController
public class CinemaAnalyticsRestController {

    private final TicketService ticketService;

    public CinemaAnalyticsRestController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping({
            "/api/cinema/films/max-viewers-summary",
            "/api/tickets/analytics/max-viewers/by-films"
    })
    public List<FilmMaxViewersSummaryDto> filmMaxViewersSummary(
            @RequestParam(required = false, defaultValue = "1000") int limit
    ) {
        return ticketService.getFilmMaxViewersSummary(limit);
    }
}
