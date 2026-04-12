package ru.hse.lab2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.hse.lab2.entity.Viewer;
import java.util.Optional;

public interface ViewerRepository extends JpaRepository<Viewer, Long> {
    Optional<Viewer> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
}