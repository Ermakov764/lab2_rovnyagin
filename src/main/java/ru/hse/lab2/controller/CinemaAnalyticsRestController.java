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
 * «Лицо» CRUD для части сценариев лаб. 8: клиент (браузер, k6) стучится сюда, на порт <b>8080</b>.
 *
 * <p><b>Что открыто снаружи:</b>
 * <ul>
 *   <li>{@code GET /api/cinema/ping} — проверка живости.</li>
 *   <li>{@code /api/cinema/films/max-viewers-summary} и алиас {@code /api/tickets/analytics/max-viewers/by-films} —
 *       прокси на Additional {@code GET /api/analytics/films/max-viewers-summary} с тем же query-параметром {@code limit}
 *       (если не указан — default только в Additional).</li>
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
            value = {
                    "/api/cinema/films/max-viewers-summary",
                    "/api/tickets/analytics/max-viewers/by-films"
            },
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public String filmMaxViewersSummaryProxy(
            @RequestParam(required = false) Integer limit
    ) {
        String base = additionalServiceBaseUrl.replaceAll("/+$", "");
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(base)
                .path("/api/analytics/films/max-viewers-summary");
        if (limit != null) {
            uriBuilder.queryParam("limit", limit);
        }
        URI uri = uriBuilder.build(true).toUri();
        String body = additionalServiceRestTemplate.getForObject(uri, String.class);
        return body != null ? body : "[]";
    }
}
