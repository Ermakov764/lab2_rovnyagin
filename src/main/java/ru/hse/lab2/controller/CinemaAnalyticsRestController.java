package ru.hse.lab2.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * GET /api/cinema/films/max-viewers-summary — прокси на AdditionalService: там сборка Summary и вызов
 * {@link ru.hse.lab2.controller.InternalFilmAnalyticsRestController} через {@code MainCrudTicketClient}.
 */
@RestController
public class CinemaAnalyticsRestController {

    private final RestTemplate additionalServiceRestTemplate;
    private final String additionalServiceBaseUrl;

    public CinemaAnalyticsRestController(
            @Qualifier("additionalServiceRestTemplate") RestTemplate additionalServiceRestTemplate,
            @Value("${additional.service.base-url:http://additional-app:8081}") String additionalServiceBaseUrl
    ) {
        this.additionalServiceRestTemplate = additionalServiceRestTemplate;
        this.additionalServiceBaseUrl = additionalServiceBaseUrl;
    }

    @GetMapping("/api/cinema/ping")
    public String ping() {
        return "ok";
    }

    @GetMapping(
            value = {
                    "/api/cinema/films/max-viewers-summary",
                    "/api/tickets/analytics/max-viewers/by-films"
            },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String filmMaxViewersSummaryProxy(
            @RequestParam(required = false, defaultValue = "1000") int limit
    ) {
        String base = additionalServiceBaseUrl.replaceAll("/+$", "");
        URI uri = UriComponentsBuilder.fromUriString(base)
                .path("/api/analytics/films/max-viewers-summary")
                .queryParam("limit", limit)
                .build(true)
                .toUri();
        String body = additionalServiceRestTemplate.getForObject(uri, String.class);
        return body != null ? body : "[]";
    }
}
