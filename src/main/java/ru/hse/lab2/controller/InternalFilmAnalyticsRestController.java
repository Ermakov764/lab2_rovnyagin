package ru.hse.lab2.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.lab2.api.dto.FilmMaxViewerRawRowDto;
import ru.hse.lab2.service.TicketAnalyticsReadService;

import java.util.List;

/** Внутренний JSON для Additional (RestTemplate), не публиковать во внешней сети без gateway. */
@RestController
@RequestMapping("/api/internal/cinema")
public class InternalFilmAnalyticsRestController {

    private final TicketAnalyticsReadService ticketAnalyticsReadService;

    public InternalFilmAnalyticsRestController(TicketAnalyticsReadService ticketAnalyticsReadService) {
        this.ticketAnalyticsReadService = ticketAnalyticsReadService;
    }

    @GetMapping("/film-max-viewer-rows")
    public List<FilmMaxViewerRawRowDto> filmMaxViewerRows(
            @RequestParam(required = false, defaultValue = "1000") int limit
    ) {
        return ticketAnalyticsReadService.filmMaxViewerRawRows(limit);
    }
}
