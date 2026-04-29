package ru.hse.lab8.additional.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import ru.hse.lab8.additional.api.MaxViewersByFilmTitleResponse;
import ru.hse.lab8.additional.config.MainCrudProperties;
import ru.hse.lab8.additional.dto.FilmDto;
import ru.hse.lab8.additional.dto.MaxViewersPerDayDto;

import java.util.List;

@Service
public class FilmTitleAnalyticsService {

    private final RestTemplate restTemplate;
    private final MainCrudProperties mainCrud;

    public FilmTitleAnalyticsService(RestTemplate mainCrudRestTemplate, MainCrudProperties mainCrud) {
        this.restTemplate = mainCrudRestTemplate;
        this.mainCrud = mainCrud;
    }

    /**
     * Join в Java: список фильмов → совпадение по названию → аналитика по filmId через основной сервис.
     */
    public MaxViewersByFilmTitleResponse maxViewersByFilmTitle(String filmTitleRaw) {
        String filmTitle = filmTitleRaw == null ? "" : filmTitleRaw.trim();
        if (filmTitle.isEmpty()) {
            throw new IllegalArgumentException("filmTitle must not be blank");
        }

        String base = mainCrud.getBaseUrl();
        List<FilmDto> films = restTemplate.exchange(
                base + "/api/films",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<FilmDto>>() {}
        ).getBody();

        if (films == null || films.isEmpty()) {
            throw new IllegalStateException("Main CRUD returned empty film list");
        }

        FilmDto film = films.stream()
                .filter(f -> f.getTitle() != null && filmTitle.equalsIgnoreCase(f.getTitle().trim()))
                .findFirst()
                .orElseThrow(() -> new FilmNotFoundException("Film not found by title: " + filmTitle));

        URI analyticsUri = UriComponentsBuilder.fromUriString(base)
                .path("/api/tickets/analytics/max-viewers")
                .queryParam("filmId", film.getId())
                .build(true)
                .toUri();

        MaxViewersPerDayDto stats = restTemplate.getForObject(analyticsUri, MaxViewersPerDayDto.class);
        if (stats == null) {
            throw new IllegalStateException("Main CRUD returned empty analytics body");
        }

        long viewers = stats.getViewersCount() != null ? stats.getViewersCount() : 0L;

        return new MaxViewersByFilmTitleResponse(
                filmTitle,
                stats.getFilmId(),
                stats.getSessionDate(),
                viewers
        );
    }

    public static final class FilmNotFoundException extends RuntimeException {
        public FilmNotFoundException(String message) {
            super(message);
        }
    }
}
