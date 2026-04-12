package ru.hse.lab2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.hse.lab2.entity.Film;
import java.util.Optional;

public interface FilmRepository extends JpaRepository<Film, Long> {

    Optional<Film> findByTitle(String title);
    boolean existsByTitle(String title);
    boolean existsByTitleAndIdNot(String title, Long id);
}