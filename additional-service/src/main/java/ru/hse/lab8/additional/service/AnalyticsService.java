package ru.hse.lab8.additional.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.hse.lab8.additional.client.CinemaCrudClient;
import ru.hse.lab8.additional.dto.CrudFilm;
import ru.hse.lab8.additional.dto.CrudTicket;
import ru.hse.lab8.additional.dto.FilmStats;
import ru.hse.lab8.additional.AnalyticsException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сводная аналитика по фильмам на основе данных основного CRUD;
 * join фильмов и билетов выполняется в памяти после двух GET.
 */
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    // При одинаковом числе уникальных зрителей выбираем более раннюю дату сеанса.
    private static final Comparator<LocalDate> EARLIER_WINS_ON_TIE = Comparator.reverseOrder();

    private final CinemaCrudClient crudClient;

    /**
     * Строит сводку "фильм -> лучший день по уникальным зрителям".
     *
     * Алгоритм:
     * 1) читаем фильмы и билеты из CRUD;
     * 2) группируем билеты по filmId;
     * 3) для каждого фильма с билетами считаем max-день в {@link #buildMaxDayStat(Long, String, List)}.
     */
    public List<FilmStats> maxViewersSummary() {
        // 1) Читаем "сырые" данные из основного CRUD.
        List<CrudFilm> films = crudClient.fetchFilms();
        // Группировка нужна, чтобы быстро получить все билеты конкретного фильма по его id.
        Map<Long, List<CrudTicket>> ticketsByFilmId = crudClient.fetchAllTickets().stream()
                .filter(t -> t.filmId() != null)
                .collect(Collectors.groupingBy(CrudTicket::filmId));

        // Пустой каталог -> пустая сводка (это не ошибка API).
        if (films.isEmpty()) {
            return List.of();
        }

        // 2) Стабильный порядок ответа: сначала фильмы с валидным id, отсортированные по id.
        List<CrudFilm> filmsOrderedById = films.stream()
                .filter(film -> film.id() != null)
                .sorted(Comparator.comparing(CrudFilm::id))
                .toList();

        return filmsOrderedById.stream()
                // В сводку включаем только фильмы, у которых есть хотя бы один билет.
                .filter(film -> ticketsByFilmId.containsKey(film.id()))
                .map(film -> {
                    String filmTitle = film.title() != null ? film.title() : "";
                    List<CrudTicket> filmTickets = ticketsByFilmId.get(film.id());
                    // 3) Для каждого фильма считаем "лучший" день по уникальным зрителям.
                    return buildMaxDayStat(film.id(), filmTitle, filmTickets);
                })
                .toList();
    }

    /** День с максимумом уникальных зрителей; при равенстве — более ранняя дата сеанса. */
    private static FilmStats buildMaxDayStat(Long filmId, String filmTitle, List<CrudTicket> tickets) {
        // Ключ: день сеанса, значение: множество уникальных viewerId в этот день.
        Map<LocalDate, Set<Long>> uniqueViewerIdsByDay = new HashMap<>();
        for (CrudTicket ticket : tickets) {
            // Неполные записи не участвуют в аналитике.
            if (ticket.sessionDate() == null || ticket.viewerId() == null) {
                continue;
            }
            uniqueViewerIdsByDay
                    .computeIfAbsent(ticket.sessionDate(), ignored -> new HashSet<>())
                    .add(ticket.viewerId());
        }

        // Переходим от Set к числу уникальных зрителей в день.
        Map<LocalDate, Long> uniqueViewersByDay = uniqueViewerIdsByDay.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> (long) e.getValue().size()));

        // Если после фильтрации не осталось валидных (date + viewerId) записей, статистику строить не из чего.
        if (uniqueViewersByDay.isEmpty()) {
            throw new AnalyticsException("No session days with viewers in tickets for film " + filmId,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Map.Entry<LocalDate, Long> best = uniqueViewersByDay.entrySet().stream()
                .max(Comparator.<Map.Entry<LocalDate, Long>>comparingLong(Map.Entry::getValue)
                        // При одинаковом максимуме берём более раннюю дату.
                        .thenComparing(Map.Entry.comparingByKey(EARLIER_WINS_ON_TIE)))
                .orElseThrow();

        // Финальная DTO для ответа API.
        return new FilmStats(filmTitle, filmId, best.getKey(), best.getValue());
    }
}
