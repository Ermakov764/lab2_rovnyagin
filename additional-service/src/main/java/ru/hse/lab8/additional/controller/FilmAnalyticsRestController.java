package ru.hse.lab8.additional.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.lab8.additional.dto.FilmMaxViewersSummaryDto;
import ru.hse.lab8.additional.service.FilmViewersSummaryService;

import java.util.List;

@RestController
public class FilmAnalyticsRestController {

    private final FilmViewersSummaryService filmViewersSummaryService;

    public FilmAnalyticsRestController(FilmViewersSummaryService filmViewersSummaryService) {
        this.filmViewersSummaryService = filmViewersSummaryService;
    }

    @GetMapping("/api/analytics/films/max-viewers-summary")
    public List<FilmMaxViewersSummaryDto> maxViewersSummary(
            @RequestParam(required = false, defaultValue = "1000") int limit
    ) {
        return filmViewersSummaryService.filmMaxViewersSummary(limit);
    }
}
