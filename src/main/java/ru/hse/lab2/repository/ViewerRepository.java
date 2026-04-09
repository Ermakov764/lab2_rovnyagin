package ru.hse.lab2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.hse.lab2.entity.Viewer;

public interface ViewerRepository extends JpaRepository<Viewer, Long> {
    // Можно добавить методы, если нужно
    // Например: Viewer findByEmail(String email);
}