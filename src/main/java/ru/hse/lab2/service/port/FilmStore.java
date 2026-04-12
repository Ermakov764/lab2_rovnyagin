package ru.hse.lab2.service.port;

import ru.hse.lab2.entity.Film;

import java.util.List;
import java.util.Optional;

public interface FilmStore {
    List<Film> findAll();

    Optional<Film> findById(Long id);

    Film save(Film film);

    void delete(Film film);

    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, Long id);
}
