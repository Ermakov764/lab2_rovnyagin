package ru.hse.lab2.repository.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.hse.lab2.entity.Film;
import ru.hse.lab2.repository.FilmRepository;
import ru.hse.lab2.service.port.FilmStore;

import java.util.List;
import java.util.Optional;

@Component
@Profile("!inmemory")
public class JpaFilmStore implements FilmStore {

    private final FilmRepository filmRepository;

    public JpaFilmStore(FilmRepository filmRepository) {
        this.filmRepository = filmRepository;
    }

    @Override
    public List<Film> findAll() {
        return filmRepository.findAll();
    }

    @Override
    public Optional<Film> findById(Long id) {
        return filmRepository.findById(id);
    }

    @Override
    public Film save(Film film) {
        return filmRepository.save(film);
    }

    @Override
    public void delete(Film film) {
        filmRepository.delete(film);
    }

    @Override
    public boolean existsByTitle(String title) {
        return filmRepository.existsByTitle(title);
    }

    @Override
    public boolean existsByTitleAndIdNot(String title, Long id) {
        return filmRepository.existsByTitleAndIdNot(title, id);
    }
}
