package ru.hse.lab8.additional.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import ru.hse.lab8.additional.client.CinemaCrudClient;
import ru.hse.lab8.additional.dto.CrudFilm;
import ru.hse.lab8.additional.dto.CrudTicket;
import ru.hse.lab8.additional.dto.FilmStats;
import ru.hse.lab8.additional.AnalyticsException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private CinemaCrudClient crudClient;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void maxViewersByFilmTitle_fetchesCatalogAndAllTickets_joinsByFilmId() {
        when(crudClient.fetchFilms()).thenReturn(List.of(new CrudFilm(1L, "Интерстеллар")));
        when(crudClient.fetchAllTickets()).thenReturn(interstellarTicketsSevenUniqueSameDay());

        FilmStats response = analyticsService.maxViewersByFilmTitle("интерстеллар");

        assertThat(response.filmTitle()).isEqualTo("интерстеллар");
        assertThat(response.filmId()).isEqualTo(1L);
        assertThat(response.day()).isEqualTo(LocalDate.of(2020, 3, 1));
        assertThat(response.maxViewersOnSessionForDay()).isEqualTo(7L);
    }

    @Test
    void maxViewersByFilmTitle_whenViewersTied_prefersEarlierSessionDate() {
        when(crudClient.fetchFilms()).thenReturn(List.of(new CrudFilm(1L, "F")));

        LocalDate d1 = LocalDate.of(2020, 3, 5);
        LocalDate d2 = LocalDate.of(2020, 3, 1);
        LocalDate d3 = LocalDate.of(2020, 2, 1);
        List<CrudTicket> tickets = new ArrayList<>();
        for (long i = 1; i <= 7; i++) {
            tickets.add(new CrudTicket(1L, i, d1));
        }
        for (long i = 10; i <= 16; i++) {
            tickets.add(new CrudTicket(1L, i, d2));
        }
        for (long i = 20; i <= 24; i++) {
            tickets.add(new CrudTicket(1L, i, d3));
        }
        when(crudClient.fetchAllTickets()).thenReturn(tickets);

        FilmStats best = analyticsService.maxViewersByFilmTitle("f");

        assertThat(best.day()).isEqualTo(d2);
        assertThat(best.maxViewersOnSessionForDay()).isEqualTo(7L);
    }

    @Test
    void maxViewersByFilmTitle_whenUnknownTitle_throwsNotFound() {
        when(crudClient.fetchFilms()).thenReturn(List.of(new CrudFilm(1L, "Other")));
        when(crudClient.fetchAllTickets()).thenReturn(List.of());

        assertThatThrownBy(() -> analyticsService.maxViewersByFilmTitle("Интерстеллар"))
                .isInstanceOf(AnalyticsException.class)
                .satisfies(ex -> assertThat(((AnalyticsException) ex).getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void maxViewersByFilmTitle_whenFilmHasNoTickets_unprocessable() {
        when(crudClient.fetchFilms()).thenReturn(List.of(new CrudFilm(1L, "Solo")));
        when(crudClient.fetchAllTickets()).thenReturn(List.of());

        assertThatThrownBy(() -> analyticsService.maxViewersByFilmTitle("solo"))
                .isInstanceOf(AnalyticsException.class)
                .satisfies(ex -> assertThat(((AnalyticsException) ex).getStatus())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void maxViewersSummaries_usesMapJoin_countsFirstFilmsWithTickets() {
        when(crudClient.fetchFilms()).thenReturn(List.of(new CrudFilm(1L, "Интерстеллар")));
        when(crudClient.fetchAllTickets()).thenReturn(List.of(
                new CrudTicket(1L, 1L, LocalDate.of(2026, 4, 19)),
                new CrudTicket(1L, 2L, LocalDate.of(2026, 4, 19)),
                new CrudTicket(1L, 1L, LocalDate.of(2026, 4, 20)),
                new CrudTicket(1L, 2L, LocalDate.of(2026, 4, 20)),
                new CrudTicket(1L, 3L, LocalDate.of(2026, 4, 20)),
                new CrudTicket(1L, 4L, LocalDate.of(2026, 4, 20))
        ));

        List<FilmStats> result = analyticsService.maxViewersSummaries(500);

        assertThat(result).hasSize(1);
        FilmStats d = result.getFirst();
        assertThat(d.filmId()).isEqualTo(1L);
        assertThat(d.filmTitle()).isEqualTo("Интерстеллар");
        assertThat(d.day()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(d.maxViewersOnSessionForDay()).isEqualTo(4L);
    }

    @Test
    void maxViewersSummaries_whenViewersTied_prefersEarlierDay() {
        when(crudClient.fetchFilms()).thenReturn(List.of(new CrudFilm(1L, "A")));
        when(crudClient.fetchAllTickets()).thenReturn(List.of(
                new CrudTicket(1L, 1L, LocalDate.of(2026, 4, 22)),
                new CrudTicket(1L, 2L, LocalDate.of(2026, 4, 22)),
                new CrudTicket(1L, 3L, LocalDate.of(2026, 4, 22)),
                new CrudTicket(1L, 4L, LocalDate.of(2026, 4, 22)),
                new CrudTicket(1L, 5L, LocalDate.of(2026, 4, 22)),
                new CrudTicket(1L, 10L, LocalDate.of(2026, 4, 21)),
                new CrudTicket(1L, 11L, LocalDate.of(2026, 4, 21)),
                new CrudTicket(1L, 12L, LocalDate.of(2026, 4, 21)),
                new CrudTicket(1L, 13L, LocalDate.of(2026, 4, 21)),
                new CrudTicket(1L, 14L, LocalDate.of(2026, 4, 21))
        ));

        List<FilmStats> one = analyticsService.maxViewersSummaries(500);

        assertThat(one).hasSize(1);
        assertThat(one.getFirst().day()).isEqualTo(LocalDate.of(2026, 4, 21));
    }

    private static List<CrudTicket> interstellarTicketsSevenUniqueSameDay() {
        LocalDate day = LocalDate.of(2020, 3, 1);
        List<CrudTicket> list = new ArrayList<>();
        for (long v = 5; v <= 11; v++) {
            list.add(new CrudTicket(1L, v, day));
        }
        return list;
    }
}
