package ru.hse.lab8.additional.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hse.lab8.additional.client.MainCrudTicketClient;
import ru.hse.lab8.additional.dto.FilmMaxViewerRawRowDto;
import ru.hse.lab8.additional.dto.FilmMaxViewersSummaryDto;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmViewersSummaryServiceTest {

    @Mock
    private MainCrudTicketClient ticketClient;

    @InjectMocks
    private FilmViewersSummaryService filmViewersSummaryService;

    @Test
    void buildsSummaryFromCrudRows() {
        FilmMaxViewerRawRowDto raw = new FilmMaxViewerRawRowDto();
        raw.setFilmId(1L);
        raw.setFilmTitle("Интерстеллар");
        raw.setDay("2026-04-20");
        raw.setViewersCount(4L);
        when(ticketClient.fetchFilmMaxViewerRawRows(500)).thenReturn(List.of(raw));

        List<FilmMaxViewersSummaryDto> result = filmViewersSummaryService.filmMaxViewersSummary(500);

        assertEquals(1, result.size());
        FilmMaxViewersSummaryDto d = result.getFirst();
        assertEquals(1L, d.getFilmId());
        assertEquals("Интерстеллар", d.getFilmTitle());
        assertEquals(LocalDate.of(2026, 4, 20), d.getDay());
        assertEquals(4L, d.getMaxViewersOnSessionForDay());
    }
}
