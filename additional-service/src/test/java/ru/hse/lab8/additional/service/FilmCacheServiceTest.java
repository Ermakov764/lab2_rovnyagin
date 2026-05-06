package ru.hse.lab8.additional.service;

import org.junit.jupiter.api.Test;
import ru.hse.lab8.additional.client.CinemaCrudClient;
import ru.hse.lab8.additional.dto.CrudFilm;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FilmCacheServiceTest {

    @Test
    void getFilms_reusesCachedFilmsWithinTtl() {
        CinemaCrudClient crudClient = mock(CinemaCrudClient.class);
        when(crudClient.fetchFilms()).thenReturn(List.of(
                new CrudFilm(1L, "Интерстеллар"),
                new CrudFilm(2L, "Начало")
        ));
        FilmCacheService cacheService = new FilmCacheService(crudClient, 60_000L);

        List<CrudFilm> first = cacheService.getFilms();
        List<CrudFilm> second = cacheService.getFilms();

        assertThat(first).containsExactlyInAnyOrder(
                new CrudFilm(1L, "Интерстеллар"),
                new CrudFilm(2L, "Начало")
        );
        assertThat(second).containsExactlyInAnyOrderElementsOf(first);
        assertThat(cacheService.size()).isEqualTo(2);
        verify(crudClient, times(1)).fetchFilms();
    }

    @Test
    void getFilms_refreshesExpiredCache() throws InterruptedException {
        CinemaCrudClient crudClient = mock(CinemaCrudClient.class);
        when(crudClient.fetchFilms())
                .thenReturn(List.of(new CrudFilm(1L, "Начало")))
                .thenReturn(List.of(new CrudFilm(1L, "Начало: Director's Cut")));
        FilmCacheService cacheService = new FilmCacheService(crudClient, 1L);

        assertThat(cacheService.getFilms()).containsExactly(new CrudFilm(1L, "Начало"));
        Thread.sleep(5L);

        assertThat(cacheService.getFilms()).containsExactly(new CrudFilm(1L, "Начало: Director's Cut"));
        verify(crudClient, times(2)).fetchFilms();
    }
}
