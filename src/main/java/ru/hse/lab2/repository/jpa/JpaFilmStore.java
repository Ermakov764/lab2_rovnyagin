package ru.hse.lab2.repository.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.hse.lab2.entity.Film;
import ru.hse.lab2.observability.ObservabilityService;
import ru.hse.lab2.repository.FilmRepository;
import ru.hse.lab2.service.port.FilmStore;

import java.util.List;
import java.util.Optional;

// Реализация порта FilmStore для режима с настоящей БД.
// Spring Data JPA исполняет запросы из FilmRepository; этот класс только проксирует вызовы и добавляет замеры (observability).
// @Profile("!inmemory"): при профиле inmemory поднимается InMemoryFilmStore из другого пакета, этот бин отключается.
@Component
@Profile("!inmemory")
public class JpaFilmStore implements FilmStore {

    private final FilmRepository filmRepository;
    private final ObservabilityService observabilityService;

    public JpaFilmStore(FilmRepository filmRepository, ObservabilityService observabilityService) {
        this.filmRepository = filmRepository;
        this.observabilityService = observabilityService;
    }

    // Все строки таблицы фильмов за один SELECT (осторожно при очень больших таблицах).
    @Override
    public List<Film> findAll() {
        return timed("db.JpaFilmStore.findAll", filmRepository::findAll);
    }

    // Поиск по PK; пусто -> Optional.empty().
    @Override
    public Optional<Film> findById(Long id) {
        return timed("db.JpaFilmStore.findById", () -> filmRepository.findById(id));
    }

    // INSERT для новой сущности или UPDATE при известном id (по политике merge/persist в JPA).
    @Override
    public Film save(Film film) {
        return timed("db.JpaFilmStore.save", () -> filmRepository.save(film));
    }

    // DELETE по распакованной сущности (должен быть задан id).
    @Override
    public void delete(Film film) {
        timedVoid("db.JpaFilmStore.delete", () -> filmRepository.delete(film));
    }

    // Валидация уникальности названия при POST.
    @Override
    public boolean existsByTitle(String title) {
        return timed("db.JpaFilmStore.existsByTitle", () -> filmRepository.existsByTitle(title));
    }

    // Валидация при PUT/PATCH: тот же title разрешён для текущего id, но не для другого фильма.
    @Override
    public boolean existsByTitleAndIdNot(String title, Long id) {
        return timed("db.JpaFilmStore.existsByTitleAndIdNot", () -> filmRepository.existsByTitleAndIdNot(title, id));
    }

    // Строка operation повторяется в JSON / логах как ключ слоя db.*.
    private <T> T timed(String operation, SupplierWithException<T> action) {
        long started = observabilityService.start();
        try {
            T result = action.get(); // внутри — вызов filmRepository.*
            observabilityService.stopSuccess(operation, started);
            return result;
        } catch (RuntimeException e) {
            observabilityService.stopFailure(operation, started);
            throw e; // не скрываем сбой, только считаем в метриках
        }
    }

    private void timedVoid(String operation, Runnable action) {
        timed(operation, () -> {
            action.run();
            return null; // универсальный timed ожидает значение типа T; для void методов подставляем null
        });
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get(); // лямбда/method ref передаётся как «выполни и верни»
    }
}
