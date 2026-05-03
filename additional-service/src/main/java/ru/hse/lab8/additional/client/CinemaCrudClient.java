package ru.hse.lab8.additional.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.hse.lab8.additional.config.MainCrudProperties;
import ru.hse.lab8.additional.dto.CrudFilm;
import ru.hse.lab8.additional.dto.CrudTicket;
import ru.hse.lab8.additional.AnalyticsException;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * Единый слой вызовов к основному CRUD: только URL и десериализация; ошибки сети/HTTP — в {@link AnalyticsException}.
 */
@Component
@RequiredArgsConstructor
public class CinemaCrudClient {

    private static final String PATH_FILMS = "/api/films";
    private static final String PATH_TICKETS = "/api/tickets";

    private static final ParameterizedTypeReference<List<CrudFilm>> FILM_LIST = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<CrudTicket>> TICKET_LIST = new ParameterizedTypeReference<>() {};

    private final RestTemplate mainCrudRestTemplate;
    private final MainCrudProperties properties;

    public List<CrudFilm> fetchFilms() {
        return exchange(uriFromPath(PATH_FILMS), FILM_LIST);
    }

    public List<CrudTicket> fetchAllTickets() {
        return exchange(uriFromPath(PATH_TICKETS), TICKET_LIST);
    }

    private URI uriFromPath(String pathFromBase) {
        return UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .path(pathFromBase)
                .build(true)
                .toUri();
    }

    private <T> List<T> exchange(URI uri, ParameterizedTypeReference<List<T>> type) {
        try {
            List<T> body = mainCrudRestTemplate.exchange(uri, HttpMethod.GET, null, type).getBody();
            return body != null ? body : Collections.emptyList();
        } catch (HttpStatusCodeException e) {
            throw new AnalyticsException(
                    "Main CRUD responded with " + e.getStatusCode().value() + ": " + e.getStatusText(),
                    HttpStatus.BAD_GATEWAY);
        } catch (ResourceAccessException e) {
            throw new AnalyticsException("Cannot reach main CRUD: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        } catch (RestClientException e) {
            throw new AnalyticsException("Rest client error: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }
}
