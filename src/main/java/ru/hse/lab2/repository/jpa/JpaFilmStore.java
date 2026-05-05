package ru.hse.lab2.repository.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.hse.lab2.entity.Film;
import ru.hse.lab2.observability.ObservabilityService;
import ru.hse.lab2.repository.FilmRepository;
import ru.hse.lab2.service.port.FilmStore;

import java.util.List;
import java.util.Optional;

@Component
@Profile("!inmemory")
public class JpaFilmStore implements FilmStore {

    private final FilmRepository filmRepository;
    private final ObservabilityService observabilityService;

    public JpaFilmStore(FilmRepository filmRepository, ObservabilityService observabilityService) {
        this.filmRepository = filmRepository;
        this.observabilityService = observabilityService;
    }

    @Override
    public List<Film> findAll() {
        return timed("db.JpaFilmStore.findAll", filmRepository::findAll);
    }

    @Override
    public Optional<Film> findById(Long id) {
        return timed("db.JpaFilmStore.findById", () -> filmRepository.findById(id));
    }

    @Override
    public Film save(Film film) {
        return timed("db.JpaFilmStore.save", () -> filmRepository.save(film));
    }

    @Override
    public void delete(Film film) {
        timedVoid("db.JpaFilmStore.delete", () -> filmRepository.delete(film));
    }

    @Override
    public boolean existsByTitle(String title) {
        return timed("db.JpaFilmStore.existsByTitle", () -> filmRepository.existsByTitle(title));
    }

    @Override
    public boolean existsByTitleAndIdNot(String title, Long id) {
        return timed("db.JpaFilmStore.existsByTitleAndIdNot", () -> filmRepository.existsByTitleAndIdNot(title, id));
    }

    private <T> T timed(String operation, SupplierWithException<T> action) {
        long started = observabilityService.start();
        try {
            T result = action.get();
            observabilityService.stopSuccess(operation, started);
            return result;
        } catch (RuntimeException e) {
            observabilityService.stopFailure(operation, started);
            throw e;
        }
    }

    private void timedVoid(String operation, Runnable action) {
        timed(operation, () -> {
            action.run();
            return null;
        });
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get();
    }
}
