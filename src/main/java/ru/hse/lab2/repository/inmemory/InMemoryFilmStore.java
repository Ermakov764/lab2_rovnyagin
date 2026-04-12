package ru.hse.lab2.repository.inmemory;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.hse.lab2.entity.Film;
import ru.hse.lab2.service.port.FilmStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Profile("inmemory")
public class InMemoryFilmStore implements FilmStore {

    private final Map<Long, Film> films = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public synchronized List<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public synchronized Optional<Film> findById(Long id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public synchronized Film save(Film film) {
        if (film.getId() == null) {
            film.setId(sequence.incrementAndGet());
        }
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public synchronized void delete(Film film) {
        films.remove(film.getId());
    }

    @Override
    public synchronized boolean existsByTitle(String title) {
        return films.values().stream().anyMatch(it -> it.getTitle().equals(title));
    }

    @Override
    public synchronized boolean existsByTitleAndIdNot(String title, Long id) {
        return films.values().stream().anyMatch(it -> it.getTitle().equals(title) && !it.getId().equals(id));
    }
}
