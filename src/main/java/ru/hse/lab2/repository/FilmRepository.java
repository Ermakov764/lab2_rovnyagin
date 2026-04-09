package ru.hse.lab2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.hse.lab2.entity.Film;
import java.util.Optional;

public interface FilmRepository extends JpaRepository<Film, Long> {

    // Найти фильм по названию
    Optional<Film> findByTitle(String title);

    // Найти все фильмы жанра
    // List<Film> findByGenre(String genre);
}