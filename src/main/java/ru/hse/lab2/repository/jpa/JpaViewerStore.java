package ru.hse.lab2.repository.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.hse.lab2.entity.Viewer;
import ru.hse.lab2.repository.ViewerRepository;
import ru.hse.lab2.service.port.ViewerStore;

import java.util.List;
import java.util.Optional;

@Component
@Profile("!inmemory")
public class JpaViewerStore implements ViewerStore {

    private final ViewerRepository viewerRepository;

    public JpaViewerStore(ViewerRepository viewerRepository) {
        this.viewerRepository = viewerRepository;
    }

    @Override
    public List<Viewer> findAll() {
        return viewerRepository.findAll();
    }

    @Override
    public Optional<Viewer> findById(Long id) {
        return viewerRepository.findById(id);
    }

    @Override
    public Viewer save(Viewer viewer) {
        return viewerRepository.save(viewer);
    }

    @Override
    public void delete(Viewer viewer) {
        viewerRepository.delete(viewer);
    }

    @Override
    public boolean existsByEmail(String email) {
        return viewerRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Long id) {
        return viewerRepository.existsByEmailAndIdNot(email, id);
    }
}
