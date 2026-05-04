package ru.hse.lab8.additional.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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

    /** Возвращает список фильмов с днём максимальной посещаемости уникальными зрителями. */
    @GetMapping("/films/max-viewers-summary")
    public List<FilmStats> maxViewersSummary() {
        // Контроллер только делегирует: вся бизнес-агрегация находится в сервисном слое.
        return analyticsService.maxViewersSummary();
    }
}
