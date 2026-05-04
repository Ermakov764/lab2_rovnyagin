package ru.hse.lab8.additional.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import ru.hse.lab8.additional.dto.FilmStats;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.hse.lab8.additional.service.AnalyticsService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnalyticsService analyticsService;

    @Test
    void summary_returns200AndBody() throws Exception {
        when(analyticsService.maxViewersSummary()).thenReturn(List.of(
                new FilmStats("A", 1L, LocalDate.of(2026, 4, 21), 5L)
        ));

        mockMvc.perform(get("/api/analytics/films/max-viewers-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].filmId").value(1))
                .andExpect(jsonPath("$[0].filmTitle").value("A"))
                .andExpect(jsonPath("$[0].day").value("2026-04-21"))
                .andExpect(jsonPath("$[0].maxViewersOnSessionForDay").value(5));
    }
}
