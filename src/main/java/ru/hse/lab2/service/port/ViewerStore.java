package ru.hse.lab2.service.port;

import ru.hse.lab2.entity.Viewer;

import java.util.List;
import java.util.Optional;

public interface ViewerStore {
    List<Viewer> findAll();

    Optional<Viewer> findById(Long id);

    Viewer save(Viewer viewer);

    void delete(Viewer viewer);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}
