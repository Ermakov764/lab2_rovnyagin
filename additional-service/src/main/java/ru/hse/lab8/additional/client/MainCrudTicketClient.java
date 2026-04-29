package ru.hse.lab8.additional.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import ru.hse.lab8.additional.config.MainCrudProperties;
import ru.hse.lab8.additional.dto.FilmMaxViewerRawRowDto;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * HTTP-клиент к основному CRUD (лаб. 8 RestTemplate как в ТЗ).
 */
@Component
public class MainCrudTicketClient {

    private final RestTemplate restTemplate;
    private final MainCrudProperties mainCrud;

    public MainCrudTicketClient(RestTemplate mainCrudRestTemplate, MainCrudProperties mainCrud) {
        this.restTemplate = mainCrudRestTemplate;
        this.mainCrud = mainCrud;
    }

    public List<FilmMaxViewerRawRowDto> fetchFilmMaxViewerRawRows(int limit) {
        String base = mainCrud.getBaseUrl().replaceAll("/+$", "");
        UriComponentsBuilder ub = UriComponentsBuilder.fromUriString(base)
                .path("/api/internal/cinema/film-max-viewer-rows");
        if (limit >= 1) {
            ub.queryParam("limit", limit);
        }
        URI uri = ub.build(true).toUri();

        List<FilmMaxViewerRawRowDto> body = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<FilmMaxViewerRawRowDto>>() {}
        ).getBody();
        return body != null ? body : Collections.emptyList();
    }
}
