package ru.hse.lab2.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.lab2.api.dto.FilmMaxViewerRawRowDto;
import ru.hse.lab.shared.analytics.TopFilmsReportLimits;
import ru.hse.lab2.service.TicketAnalyticsReadService;

import java.util.List;

/**
 * Служебный контроллер <b>внутренней</b> сети: базовый путь {@code /api/internal/cinema}.
 *
 * <p><b>Эндпоинт:</b>
 * <ul>
 *   <li>{@code GET /api/internal/cinema/film-max-viewer-rows?limit=} — legacy: строка на фильм из SQL;
 *       без {@code limit} — {@link ru.hse.lab.shared.analytics.TopFilmsReportLimits#DEFAULT}.
 *       Сводка Additional в лаб. 8 этот путь <b>не</b> использует; там {@code /api/films} + {@code /api/tickets?filmId=}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/internal/cinema")
public class InternalFilmAnalyticsRestController {

    private final TicketAnalyticsReadService ticketAnalyticsReadService;

    public InternalFilmAnalyticsRestController(TicketAnalyticsReadService ticketAnalyticsReadService) {
        this.ticketAnalyticsReadService = ticketAnalyticsReadService;
    }

    /** Legacy: одна строка на фильм, «лучший» день уже в SQL (LATERAL). Не используется сводкой Additional в лаб. 8. */
    @GetMapping("/film-max-viewer-rows")
    public List<FilmMaxViewerRawRowDto> filmMaxViewerRows(
            @RequestParam(required = false) Integer limit
    ) {
        return ticketAnalyticsReadService.filmMaxViewerRawRows(TopFilmsReportLimits.effectiveLimit(limit));
    }
}
