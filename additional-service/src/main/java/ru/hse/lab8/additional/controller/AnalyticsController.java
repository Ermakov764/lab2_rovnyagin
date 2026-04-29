package ru.hse.lab8.additional.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.lab8.additional.api.MaxViewersByFilmTitleResponse;
import ru.hse.lab8.additional.service.FilmTitleAnalyticsService;
import ru.hse.lab8.additional.service.FilmTitleAnalyticsService.FilmNotFoundException;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final FilmTitleAnalyticsService analyticsService;

    public AnalyticsController(FilmTitleAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Дополнительно (ТЗ кинотеатра): вход — название фильма; выход — день и максимум зрителей на сеансе в этот день.
     */
    @GetMapping("/max-viewers-by-film-title")
    public MaxViewersByFilmTitleResponse maxViewersByFilmTitle(@RequestParam("filmTitle") String filmTitle) {
        return analyticsService.maxViewersByFilmTitle(filmTitle);
    }

    @ExceptionHandler(FilmNotFoundException.class)
    public ResponseEntity<String> notFound(FilmNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}
