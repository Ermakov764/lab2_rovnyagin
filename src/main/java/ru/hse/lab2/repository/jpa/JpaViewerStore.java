package ru.hse.lab2.repository.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.hse.lab2.entity.Viewer;
import ru.hse.lab2.observability.ObservabilityService;
import ru.hse.lab2.repository.ViewerRepository;
import ru.hse.lab2.service.port.ViewerStore;

import java.util.List;
import java.util.Optional;

@Component
@Profile("!inmemory")
public class JpaViewerStore implements ViewerStore {

    private final ViewerRepository viewerRepository;
    private final ObservabilityService observabilityService;

    public JpaViewerStore(ViewerRepository viewerRepository, ObservabilityService observabilityService) {
        this.viewerRepository = viewerRepository;
        this.observabilityService = observabilityService;
    }

    @Override
    public List<Viewer> findAll() {
        return timed("db.JpaViewerStore.findAll", viewerRepository::findAll);
    }

    @Override
    public Optional<Viewer> findById(Long id) {
        return timed("db.JpaViewerStore.findById", () -> viewerRepository.findById(id));
    }

    @Override
    public Viewer save(Viewer viewer) {
        return timed("db.JpaViewerStore.save", () -> viewerRepository.save(viewer));
    }

    @Override
    public void delete(Viewer viewer) {
        timedVoid("db.JpaViewerStore.delete", () -> viewerRepository.delete(viewer));
    }

    @Override
    public boolean existsByEmail(String email) {
        return timed("db.JpaViewerStore.existsByEmail", () -> viewerRepository.existsByEmail(email));
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Long id) {
        return timed("db.JpaViewerStore.existsByEmailAndIdNot", () -> viewerRepository.existsByEmailAndIdNot(email, id));
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
