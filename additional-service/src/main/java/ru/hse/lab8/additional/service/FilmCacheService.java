package ru.hse.lab8.additional.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.hse.lab8.additional.client.CinemaCrudClient;
import ru.hse.lab8.additional.dto.CrudFilm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Кеш справочника фильмов в памяти Additional service.
 *
 * <p>Фильмы нужны аналитике как справочные данные для вывода названий, поэтому их не нужно
 * запрашивать из CRUD на каждый расчет статистики.
 */
@Service
public class FilmCacheService {

    private static final Logger log = LoggerFactory.getLogger(FilmCacheService.class);

    private final CinemaCrudClient crudClient;
    // Срок жизни кеша: после истечения TTL следующий запрос обновит фильмы из CRUD.
    private final long ttlMs;
    // Основное хранилище кеша: filmId -> снимок фильма из CRUD.
    // ConcurrentHashMap безопаснее обычного HashMap при параллельных HTTP-запросах к Additional.
    private final Map<Long, CrudFilm> filmsById = new ConcurrentHashMap<>();

    // Время последнего успешного refresh, нужно для проверки TTL.
    private long lastRefreshAtMs;
    // Счетчики нужны для логов и отчета по лабораторной.
    private long hits; // hits — сколько раз данные были успешно взяты из кеша. То есть фильмы уже лежали в памяти и TTL еще не истек.
    private long misses; // misses — сколько раз данные были не найдены в кеше. То есть TTL истек и фильмы были запрошены из CRUD.
    private long refreshes; // refreshes — сколько раз кеш был обновлен. То есть фильмы были запрошены из CRUD.

    public FilmCacheService(
            CinemaCrudClient crudClient,
            // Значение приходит из application.properties: film-cache.ttl-ms.
            @Value("${film-cache.ttl-ms:30000}") long ttlMs
    ) {
        this.crudClient = crudClient;
        // Защита от некорректной настройки: TTL не должен быть меньше 1 мс.
        this.ttlMs = Math.max(1L, ttlMs);
    }

    public synchronized List<CrudFilm> getFilms() {
        // synchronized оставлен для атомарной проверки TTL и refresh, чтобы несколько потоков не обновляли кеш одновременно.
        long nowMs = System.currentTimeMillis();
        // Если TTL еще не истек, считаем обращение успешным попаданием в кеш.
        if (!isExpired(nowMs)) {
            // Cache hit: данные еще свежие, сетевой GET /api/films не выполняется.
            hits++;
            return snapshot();
        }

        // Cache miss: кеш пустой или устарел, поэтому обновляем справочник из CRUD.
        misses++;
        refreshFromCrud(nowMs);
        return snapshot();
    }

    public synchronized int size() {
        return filmsById.size();
    }

    /**
     * Периодически выводит в лог статистику состояния кеша фильмов.
     * Аннотация @Scheduled: метод вызывается автоматически через фиксированный интервал времени, указанный в свойстве film-cache.stats-rate-ms (по умолчанию 10000 мс = 10 секунд).
     * synchronized: чтобы избежать одновременного доступа к кешу и счетчикам из разных потоков.
     */
    @Scheduled(fixedRateString = "${film-cache.stats-rate-ms:10000}")
    public synchronized void logCacheStats() {
        // В лог выводится:
        // size           — текущее количество фильмов в кеше
        // ttlMs          — установленный TTL кеша в миллисекундах
        // lastRefreshAtMs— время последнего обновления кеша (в миллисекундах с начала эпохи)
        // hits           — сколько раз успешно получали данные из кеша (без обращения к CRUD)
        // misses         — сколько раз данные пришлось запросить из CRUD (просрочен TTL)
        // refreshes      — сколько раз кеш был полностью обновлен из CRUD
        log.info(
                "film-cache stats: size={} ttlMs={} lastRefreshAtMs={} hits={} misses={} refreshes={}",
                filmsById.size(),
                ttlMs,
                lastRefreshAtMs,
                hits,
                misses,
                refreshes
        );
    }

    /**
     * Проверяет, истёк ли срок действия кеша (TTL).
     *
     * @param nowMs текущее время в миллисекундах (System.currentTimeMillis())
     * @return true, если кеш истёк или ещё не был заполнен (lastRefreshAtMs == 0L), иначе false
     *
     * Логика:
     * - Если кеш ещё ни разу не обновлялся (lastRefreshAtMs == 0L), считаем его просроченным.
     * - Иначе сравниваем разницу между текущим временем и временем последнего обновления кеша;
     *   если прошло больше или равно ttlMs миллисекунд, кеш истёк.
     */
    private boolean isExpired(long nowMs) {
        return lastRefreshAtMs == 0L || nowMs - lastRefreshAtMs >= ttlMs;
    }

    private void refreshFromCrud(long nowMs) {
        // Полностью заменяем кеш свежим снимком справочника фильмов.
        List<CrudFilm> films = crudClient.fetchFilms();
        filmsById.clear();
        for (CrudFilm film : films) {
            // Записи без id нельзя положить в map по ключу filmId.
            if (film.id() != null) {
                filmsById.put(film.id(), film);
            }
        }
        lastRefreshAtMs = nowMs;
        refreshes++;
    }

    private List<CrudFilm> snapshot() {
        // Возвращаем копию, чтобы внешний код не мог изменить внутренний ConcurrentHashMap.
        return List.copyOf(filmsById.values());
    }
}
