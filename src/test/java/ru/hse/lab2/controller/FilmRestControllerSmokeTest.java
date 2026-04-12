package ru.hse.lab2.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.hse.lab2.api.dto.FilmDto;
import ru.hse.lab2.service.FilmService;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FilmRestControllerSmokeTest {

    @Test
    void getAll_returnsOkAndFilmPayload() throws Exception {
        FilmService filmService = mock(FilmService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new FilmRestController(filmService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        when(filmService.getAll()).thenReturn(List.of(
                new FilmDto(101L, "Interstellar", "Sci-Fi", 169)
        ));

        mockMvc.perform(get("/api/films"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].title").value("Interstellar"));
    }
}
