package ru.hse.lab8.additional.controller;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.lab.shared.analytics.TopFilmsReportLimits;
import ru.hse.lab8.additional.dto.FilmStats;
import ru.hse.lab8.additional.service.AnalyticsService;

import java.util.List;

/**
 * Единая точка REST для аналитики по данным основного CRUD.
 */
@Validated
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/max-viewers-by-film-title")
    public FilmStats maxViewersByFilmTitle(@RequestParam("filmTitle") @NotBlank String filmTitle) {
        return analyticsService.maxViewersByFilmTitle(filmTitle);
    }

    @GetMapping("/films/max-viewers-summary")
    public List<FilmStats> maxViewersSummary(@RequestParam(required = false) Integer limit) {
        int effective = TopFilmsReportLimits.effectiveLimit(limit);
        return analyticsService.maxViewersSummaries(effective);
    }
}
