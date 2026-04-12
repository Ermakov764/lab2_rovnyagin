package ru.hse.lab2.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hse.lab2.api.dto.MaxViewersPerDayDto;
import ru.hse.lab2.api.request.TicketRequest;
import ru.hse.lab2.entity.Film;
import ru.hse.lab2.entity.Viewer;
import ru.hse.lab2.exception.ConflictException;
import ru.hse.lab2.service.port.TicketStore;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketStore ticketStore;

    @Mock
    private ViewerService viewerService;

    @Mock
    private FilmService filmService;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void getMaxViewersPerDayByFilm_returnsDayWithMaximumViewers() {
        Film film = new Film();
        film.setId(7L);
        LocalDate lowerDate = LocalDate.of(2026, 4, 1);
        LocalDate topDate = LocalDate.of(2026, 4, 2);

        when(filmService.getEntityById(7L)).thenReturn(film);
        when(ticketStore.findDailyViewerStatsByFilmId(7L))
                .thenReturn(List.of(
                        new Object[]{topDate, 12L},
                        new Object[]{lowerDate, 9L}
                ));

        MaxViewersPerDayDto result = ticketService.getMaxViewersPerDayByFilm(7L);

        assertEquals(7L, result.getFilmId());
        assertEquals(topDate, result.getSessionDate());
        assertEquals(12L, result.getViewersCount());
    }

    @Test
    void getMaxViewersPerDayByFilm_usesEarlierDateAsTieBreaker() {
        Film film = new Film();
        film.setId(7L);
        LocalDate earlierDate = LocalDate.of(2026, 4, 1);
        LocalDate laterDate = LocalDate.of(2026, 4, 2);

        when(filmService.getEntityById(7L)).thenReturn(film);
        when(ticketStore.findDailyViewerStatsByFilmId(7L))
                .thenReturn(List.of(
                        new Object[]{earlierDate, 10L},
                        new Object[]{laterDate, 10L}
                ));

        MaxViewersPerDayDto result = ticketService.getMaxViewersPerDayByFilm(7L);

        assertEquals(earlierDate, result.getSessionDate());
        assertEquals(10L, result.getViewersCount());
    }

    @Test
    void create_throwsConflictWhenSeatAlreadyTaken() {
        TicketRequest request = new TicketRequest(
                1L,
                2L,
                LocalDate.of(2026, 4, 2),
                LocalTime.of(19, 30),
                "A-10",
                450.0
        );

        Viewer viewer = new Viewer();
        viewer.setId(1L);
        Film film = new Film();
        film.setId(2L);

        when(viewerService.getEntityById(1L)).thenReturn(viewer);
        when(filmService.getEntityById(2L)).thenReturn(film);
        when(ticketStore.existsByFilmSessionAndSeat(2L, request.getSessionDate(), request.getSessionTime(), "A-10"))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> ticketService.create(request));
        verify(ticketStore, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
