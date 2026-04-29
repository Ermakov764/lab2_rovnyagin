package ru.hse.lab8.additional.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Доказывает, что Additional не ходит в БД: два HTTP-вызова к основному CRUD
 * (список фильмов → аналитика по filmId), как в {@link FilmTitleAnalyticsService}.
 */
@SpringBootTest
class FilmTitleAnalyticsServiceTest {

    @Autowired
    private FilmTitleAnalyticsService analyticsService;

    @Autowired
    private RestTemplate mainCrudRestTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.bindTo(mainCrudRestTemplate).build();
    }

    @AfterEach
    void verifyServer() {
        mockServer.verify();
    }

    @Test
    void pullsFilmsThenMaxViewersByFilmId_viaRestTemplate() {
        mockServer.expect(requestTo("http://localhost:8080/api/films"))
                .andRespond(withSuccess(
                        "[{\"id\":1,\"title\":\"Интерстеллар\",\"genre\":\"Sci-Fi\",\"durationMinutes\":169}]",
                        MediaType.APPLICATION_JSON
                ));
        mockServer.expect(requestTo(
                        "http://localhost:8080/api/tickets/analytics/max-viewers?filmId=1"))
                .andRespond(withSuccess(
                        "{\"filmId\":1,\"sessionDate\":\"2020-03-01\",\"viewersCount\":7}",
                        MediaType.APPLICATION_JSON
                ));

        var response = analyticsService.maxViewersByFilmTitle("интерстеллар");

        assertThat(response.filmTitle()).isEqualTo("интерстеллар");
        assertThat(response.filmId()).isEqualTo(1L);
        assertThat(response.day().toString()).isEqualTo("2020-03-01");
        assertThat(response.maxViewersOnSessionForDay()).isEqualTo(7L);
    }

    @Test
    void throwsWhenFilmTitleNotInFilmList() {
        mockServer.expect(requestTo("http://localhost:8080/api/films"))
                .andRespond(withSuccess("[{\"id\":1,\"title\":\"Other\",\"genre\":null,\"durationMinutes\":null}]",
                        MediaType.APPLICATION_JSON));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> analyticsService.maxViewersByFilmTitle("Интерстеллар"))
                .isInstanceOf(FilmTitleAnalyticsService.FilmNotFoundException.class);
    }
}
