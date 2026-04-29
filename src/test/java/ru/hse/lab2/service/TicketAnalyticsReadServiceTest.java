package ru.hse.lab2.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hse.lab2.api.dto.FilmMaxViewerRawRowDto;
import ru.hse.lab2.service.port.TicketStore;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketAnalyticsReadServiceTest {

    @Mock
    private TicketStore ticketStore;

    @InjectMocks
    private TicketAnalyticsReadService ticketAnalyticsReadService;

    @Test
    void filmMaxViewerRawRows_mapsAggregates() {
        when(ticketStore.findAllFilmDailyViewerAggregates(1000))
                .thenReturn(List.of(
                        new Object[]{1L, "A", LocalDate.of(2026, 4, 11), 12L},
                        new Object[]{2L, "B", LocalDate.of(2026, 5, 1), 3L}
                ));

        List<FilmMaxViewerRawRowDto> result = ticketAnalyticsReadService.filmMaxViewerRawRows(1000);

        assertEquals(2, result.size());
        FilmMaxViewerRawRowDto f1 = result.stream().filter(r -> r.filmId() == 1L).findFirst().orElseThrow();
        assertEquals("A", f1.filmTitle());
        assertEquals("2026-04-11", f1.day());
        assertEquals(12L, f1.viewersCount());
    }
}
