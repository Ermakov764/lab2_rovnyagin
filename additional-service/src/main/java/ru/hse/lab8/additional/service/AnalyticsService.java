package ru.hse.lab8.additional.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.hse.lab.shared.analytics.TopFilmsReportLimits;
import ru.hse.lab8.additional.client.CinemaCrudClient;
import ru.hse.lab8.additional.dto.CrudFilm;
import ru.hse.lab8.additional.dto.CrudTicket;
import ru.hse.lab8.additional.dto.FilmStats;
import ru.hse.lab8.additional.AnalyticsException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Вся аналитика additional-service: два публичных сценария и приватные шаги в одном месте.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private static final Comparator<LocalDate> EARLIER_WINS_ON_TIE = Comparator.reverseOrder();

    private final CinemaCrudClient crudClient;

    /**
     * @param filmTitleParameter не пустой (проверка на границе — {@link jakarta.validation.constraints.NotBlank} в контроллере).
     */
    public FilmStats maxViewersByFilmTitle(String filmTitleParameter) {
        String requestedTitle = filmTitleParameter.trim();

        CatalogSnapshot snapshot = CatalogSnapshot.load(crudClient);
        List<CrudFilm> catalog = snapshot.catalog();
        if (catalog.isEmpty()) {
            throw new AnalyticsException("Main CRUD returned empty film list", HttpStatus.BAD_GATEWAY);
        }

        CrudFilm film = findFilmByExactTitleCaseInsensitive(catalog, requestedTitle)
                .orElseThrow(() -> new AnalyticsException("Film not found by title: " + requestedTitle, HttpStatus.NOT_FOUND));
        if (film.id() == null) {
            throw new AnalyticsException("Film catalog entry has null id for title: " + requestedTitle, HttpStatus.BAD_GATEWAY);
        }

        List<CrudTicket> tickets = snapshot.ticketsByFilmId().getOrDefault(film.id(), List.of());
        if (tickets.isEmpty()) {
            throw new AnalyticsException("No tickets for film " + film.id(), HttpStatus.UNPROCESSABLE_ENTITY);
        }

        return bestFilmStats(film.id(), requestedTitle, tickets);
    }

    public List<FilmStats> maxViewersSummaries(int limit) {
        try {
            TopFilmsReportLimits.requireInRange(limit);
        } catch (IllegalArgumentException e) {
            throw new AnalyticsException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        CatalogSnapshot snapshot = CatalogSnapshot.load(crudClient);
        List<CrudFilm> catalog = snapshot.catalog();
        if (catalog.isEmpty()) {
            return List.of();
        }

        Map<Long, List<CrudTicket>> byFilmId = snapshot.ticketsByFilmId();

        List<CrudFilm> filmsOrderedById = catalog.stream()
                .filter(f -> f.id() != null)
                .sorted(Comparator.comparing(CrudFilm::id))
                .toList();

        return filmsOrderedById.stream()
                .filter(f -> !byFilmId.getOrDefault(f.id(), List.of()).isEmpty())
                .limit(limit)
                .map(f -> bestFilmStats(
                        f.id(),
                        f.title() != null ? f.title() : "",
                        byFilmId.get(f.id())))
                .toList();
    }

    /**
     * Два запроса к CRUD и один проход для join: без повторения вызовов клиента в каждом сценарии.
     */
    private record CatalogSnapshot(List<CrudFilm> catalog, Map<Long, List<CrudTicket>> ticketsByFilmId) {
        static CatalogSnapshot load(CinemaCrudClient client) {
            List<CrudFilm> catalog = client.fetchFilms();
            Map<Long, List<CrudTicket>> byFilmId = groupTicketsByFilmId(client.fetchAllTickets());
            return new CatalogSnapshot(catalog, byFilmId);
        }
    }

    private static Map<Long, List<CrudTicket>> groupTicketsByFilmId(List<CrudTicket> allTickets) {
        return allTickets.stream()
                .filter(t -> t.filmId() != null)
                .collect(Collectors.groupingBy(CrudTicket::filmId));
    }

    private static Optional<CrudFilm> findFilmByExactTitleCaseInsensitive(List<CrudFilm> films, String titleFromRequest) {
        return films.stream()
                .filter(f -> f.title() != null && titleFromRequest.equalsIgnoreCase(f.title().trim()))
                .findFirst();
    }

    private static FilmStats bestFilmStats(Long filmId, String filmTitleForResponse, List<CrudTicket> tickets) {
        Map<LocalDate, Long> uniqueViewersByDay = tickets.stream()
                .filter(t -> t.sessionDate() != null && t.viewerId() != null)
                .collect(Collectors.groupingBy(
                        CrudTicket::sessionDate,
                        Collectors.mapping(CrudTicket::viewerId, Collectors.toCollection(HashSet::new))))
                .entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> (long) e.getValue().size()));

        if (uniqueViewersByDay.isEmpty()) {
            throw new AnalyticsException("No session days with viewers in tickets for film " + filmId,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Map.Entry<LocalDate, Long> best = uniqueViewersByDay.entrySet().stream()
                .max(Comparator.<Map.Entry<LocalDate, Long>>comparingLong(Map.Entry::getValue)
                        .thenComparing(Map.Entry.comparingByKey(EARLIER_WINS_ON_TIE)))
                .orElseThrow();

        return new FilmStats(filmTitleForResponse, filmId, best.getKey(), best.getValue());
    }
}
