package ru.hse.lab2.repository.inmemory;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.hse.lab2.entity.Viewer;
import ru.hse.lab2.service.port.ViewerStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Profile("inmemory")
public class InMemoryViewerStore implements ViewerStore {

    private final Map<Long, Viewer> viewers = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public synchronized List<Viewer> findAll() {
        return new ArrayList<>(viewers.values());
    }

    @Override
    public synchronized Optional<Viewer> findById(Long id) {
        return Optional.ofNullable(viewers.get(id));
    }

    @Override
    public synchronized Viewer save(Viewer viewer) {
        if (viewer.getId() == null) {
            viewer.setId(sequence.incrementAndGet());
        }
        viewers.put(viewer.getId(), viewer);
        return viewer;
    }

    @Override
    public synchronized void delete(Viewer viewer) {
        viewers.remove(viewer.getId());
    }

    @Override
    public synchronized boolean existsByEmail(String email) {
        return viewers.values().stream().anyMatch(it -> it.getEmail().equals(email));
    }

    @Override
    public synchronized boolean existsByEmailAndIdNot(String email, Long id) {
        return viewers.values().stream().anyMatch(it -> it.getEmail().equals(email) && !it.getId().equals(id));
    }
}
