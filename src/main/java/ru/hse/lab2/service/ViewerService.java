package ru.hse.lab2.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hse.lab2.api.dto.ViewerDto;
import ru.hse.lab2.api.mapper.ViewerMapper;
import ru.hse.lab2.api.request.ViewerRequest;
import ru.hse.lab2.entity.Viewer;
import ru.hse.lab2.exception.ConflictException;
import ru.hse.lab2.exception.NotFoundException;
import ru.hse.lab2.exception.ValidationException;
import ru.hse.lab2.service.port.TicketStore;
import ru.hse.lab2.service.port.ViewerStore;

import java.util.List;

@Service
public class ViewerService {

    private final ViewerStore viewerStore;
    private final TicketStore ticketStore;

    public ViewerService(ViewerStore viewerStore, TicketStore ticketStore) {
        this.viewerStore = viewerStore;
        this.ticketStore = ticketStore;
    }

    @Transactional(readOnly = true)
    public List<ViewerDto> getAll() {
        return viewerStore.findAll().stream()
                .map(ViewerMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public ViewerDto getById(Long id) {
        return ViewerMapper.toDto(getEntityById(id));
    }

    @Transactional
    public ViewerDto create(ViewerRequest request) {
        validateRequest(request);
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (viewerStore.existsByEmail(normalizedEmail)) {
            throw new ConflictException("Viewer with email '" + request.getEmail() + "' already exists");
        }

        Viewer viewer = ViewerMapper.toEntity(request);
        viewer.setName(request.getName().trim());
        viewer.setEmail(normalizedEmail);
        try {
            return ViewerMapper.toDto(viewerStore.save(viewer));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Viewer with email '" + request.getEmail() + "' already exists");
        }
    }

    @Transactional
    public ViewerDto update(Long id, ViewerRequest request) {
        validateRequest(request);
        Viewer viewer = getEntityById(id);
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (viewerStore.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new ConflictException("Viewer with email '" + request.getEmail() + "' already exists");
        }

        ViewerMapper.updateEntity(viewer, request);
        viewer.setName(request.getName().trim());
        viewer.setEmail(normalizedEmail);
        try {
            return ViewerMapper.toDto(viewerStore.save(viewer));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Viewer with email '" + request.getEmail() + "' already exists");
        }
    }

    @Transactional
    public void delete(Long id) {
        Viewer viewer = getEntityById(id);
        ticketStore.deleteByViewerId(id);
        viewerStore.delete(viewer);
    }

    @Transactional(readOnly = true)
    public Viewer getEntityById(Long id) {
        if (id == null) {
            throw new ValidationException("Viewer id must not be null");
        }
        return viewerStore.findById(id)
                .orElseThrow(() -> new NotFoundException("Viewer with id " + id + " not found"));
    }

    private void validateRequest(ViewerRequest request) {
        if (request == null) {
            throw new ValidationException("Viewer payload must not be null");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new ValidationException("Viewer name must not be blank");
        }
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new ValidationException("Viewer email must not be blank");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
