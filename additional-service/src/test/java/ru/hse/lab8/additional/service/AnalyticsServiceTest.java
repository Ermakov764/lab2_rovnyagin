package ru.hse.lab8.additional.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hse.lab8.additional.client.CinemaCrudClient;
import ru.hse.lab8.additional.dto.CrudFilm;
import ru.hse.lab8.additional.dto.CrudTicket;
import ru.hse.lab8.additional.dto.FilmStats;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private CinemaCrudClient crudClient;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void maxViewersSummary_usesMapJoin_countsFirstFilmsWithTickets() {
        when(crudClient.fetchFilms()).thenReturn(List.of(new CrudFilm(1L, "Интерстеллар")));
        when(crudClient.fetchAllTickets()).thenReturn(List.of(
                new CrudTicket(1L, 1L, LocalDate.of(2026, 4, 19)),
                new CrudTicket(1L, 2L, LocalDate.of(2026, 4, 19)),
                new CrudTicket(1L, 1L, LocalDate.of(2026, 4, 20)),
                new CrudTicket(1L, 2L, LocalDate.of(2026, 4, 20)),
                new CrudTicket(1L, 3L, LocalDate.of(2026, 4, 20)),
                new CrudTicket(1L, 4L, LocalDate.of(2026, 4, 20))
        ));

        List<FilmStats> result = analyticsService.maxViewersSummary();

        assertThat(result).hasSize(1);
        FilmStats d = result.getFirst();
        assertThat(d.filmId()).isEqualTo(1L);
        assertThat(d.filmTitle()).isEqualTo("Интерстеллар");
        assertThat(d.day()).isEqualTo(LocalDate.of(2026, 4, 20));
        assertThat(d.maxViewersOnSessionForDay()).isEqualTo(4L);
    }

    @Test
    void maxViewersSummary_whenViewersTied_prefersEarlierDay() {
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

        List<FilmStats> one = analyticsService.maxViewersSummary();

        assertThat(one).hasSize(1);
        assertThat(one.getFirst().day()).isEqualTo(LocalDate.of(2026, 4, 21));
    }
}
