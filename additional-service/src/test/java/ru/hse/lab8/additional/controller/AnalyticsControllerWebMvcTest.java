package ru.hse.lab8.additional.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.hse.lab8.additional.AnalyticsException;
import ru.hse.lab8.additional.service.AnalyticsService;

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
    void blankFilmTitle_returns400ProblemDetail() throws Exception {
        mockMvc.perform(get("/api/analytics/max-viewers-by-film-title")
                        .param("filmTitle", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void notFound_returns404ProblemDetail() throws Exception {
        when(analyticsService.maxViewersByFilmTitle("x")).thenThrow(
                new AnalyticsException("Film not found by title: x", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/analytics/max-viewers-by-film-title").param("filmTitle", "x"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Film not found by title: x"));
    }
}
