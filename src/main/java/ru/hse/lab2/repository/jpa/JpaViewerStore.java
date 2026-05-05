package ru.hse.lab2.repository.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.hse.lab2.entity.Viewer;
import ru.hse.lab2.observability.ObservabilityService;
import ru.hse.lab2.repository.ViewerRepository;
import ru.hse.lab2.service.port.ViewerStore;

import java.util.List;
import java.util.Optional;

// Реализация ViewerStore через JPA: CRUD по таблице зрителей через ViewerRepository.
// Альтернатива при @Profile("inmemory") — in-memory коллекции в другом классе (этот компонент тогда выключен).
@Component
@Profile("!inmemory")
public class JpaViewerStore implements ViewerStore {

    private final ViewerRepository viewerRepository;
    private final ObservabilityService observabilityService;

    public JpaViewerStore(ViewerRepository viewerRepository, ObservabilityService observabilityService) {
        this.viewerRepository = viewerRepository;
        this.observabilityService = observabilityService;
    }

    // Все зрители из БД.
    @Override
    public List<Viewer> findAll() {
        return timed("db.JpaViewerStore.findAll", viewerRepository::findAll);
    }

    @Override
    public Optional<Viewer> findById(Long id) {
        return timed("db.JpaViewerStore.findById", () -> viewerRepository.findById(id));
    }

    // Частый путь под POST /api/viewers или обновление профиля.
    @Override
    public Viewer save(Viewer viewer) {
        return timed("db.JpaViewerStore.save", () -> viewerRepository.save(viewer));
    }

    @Override
    public void delete(Viewer viewer) {
        timedVoid("db.JpaViewerStore.delete", () -> viewerRepository.delete(viewer));
    }

    // Проверка «email уже занят» при регистрации (уникальность в БД + дублирование в API).
    @Override
    public boolean existsByEmail(String email) {
        return timed("db.JpaViewerStore.existsByEmail", () -> viewerRepository.existsByEmail(email));
    }

    // Редактирование: разрешить оставить свой email, но запретить совпадение с чужим viewer id.
    @Override
    public boolean existsByEmailAndIdNot(String email, Long id) {
        return timed("db.JpaViewerStore.existsByEmailAndIdNot", () -> viewerRepository.existsByEmailAndIdNot(email, id));
    }

    // Имя операции — стабильный ключ в /api/observability (префикс db.JpaViewerStore.*).
    private <T> T timed(String operation, SupplierWithException<T> action) {
        long started = observabilityService.start();
        try {
            T result = action.get(); // любой метод viewerRepository.*
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
            return null; // см. общий метод timed(...) выше
        });
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get(); // эквивалент Supplier без привязки к стандартной библиотеке
    }
}
