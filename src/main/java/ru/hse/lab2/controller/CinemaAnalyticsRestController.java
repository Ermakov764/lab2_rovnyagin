package ru.hse.lab2.controller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * «Лицо» CRUD для части сценариев лаб. 8: клиент (браузер, k6) стучится сюда, на порт <b>8080</b>.
 *
 * <p><b>Что открыто снаружи:</b>
 * <ul>
 *   <li>{@code GET /api/cinema/ping} — проверка живости.</li>
 *   <li>{@code GET /api/cinema/films/max-viewers-summary} —
 *       прокси на Additional {@code GET /api/analytics/films/max-viewers-summary}.</li>
 * </ul>
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
            value = "/api/cinema/films/max-viewers-summary",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String filmMaxViewersSummaryProxy() {
        String base = additionalServiceBaseUrl.replaceAll("/+$", "");
        URI uri = UriComponentsBuilder.fromUriString(base)
                .path("/api/analytics/films/max-viewers-summary")
                .build(true)
                .toUri();
        String body = additionalServiceRestTemplate.getForObject(uri, String.class);
        return body != null ? body : "[]";
    }
}
