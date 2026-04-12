package ru.hse.lab2.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hse.lab2.api.dto.FilmDto;
import ru.hse.lab2.api.mapper.FilmMapper;
import ru.hse.lab2.api.request.FilmRequest;
import ru.hse.lab2.entity.Film;
import ru.hse.lab2.exception.ConflictException;
import ru.hse.lab2.exception.NotFoundException;
import ru.hse.lab2.exception.ValidationException;
import ru.hse.lab2.service.port.FilmStore;
import ru.hse.lab2.service.port.TicketStore;

import java.util.List;

@Service
public class FilmService {

    private final FilmStore filmStore;
    private final TicketStore ticketStore;

    public FilmService(FilmStore filmStore, TicketStore ticketStore) {
        this.filmStore = filmStore;
        this.ticketStore = ticketStore;
    }

    @Transactional(readOnly = true)
    public List<FilmDto> getAll() {
        return filmStore.findAll().stream()
                .map(FilmMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public FilmDto getById(Long id) {
        return FilmMapper.toDto(getEntityById(id));
    }

    @Transactional
    public FilmDto create(FilmRequest request) {
        validateRequest(request);
        String normalizedTitle = request.getTitle().trim();
        if (filmStore.existsByTitle(normalizedTitle)) {
            throw new ConflictException("Film with title '" + request.getTitle() + "' already exists");
        }

        Film film = FilmMapper.toEntity(request);
        film.setTitle(normalizedTitle);
        try {
            return FilmMapper.toDto(filmStore.save(film));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Film with title '" + request.getTitle() + "' already exists");
        }
    }

    @Transactional
    public FilmDto update(Long id, FilmRequest request) {
        validateRequest(request);
        Film film = getEntityById(id);
        String normalizedTitle = request.getTitle().trim();
        if (filmStore.existsByTitleAndIdNot(normalizedTitle, id)) {
            throw new ConflictException("Film with title '" + request.getTitle() + "' already exists");
        }

        FilmMapper.updateEntity(film, request);
        film.setTitle(normalizedTitle);
        try {
            return FilmMapper.toDto(filmStore.save(film));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Film with title '" + request.getTitle() + "' already exists");
        }
    }

    @Transactional
    public void delete(Long id) {
        Film film = getEntityById(id);
        ticketStore.deleteByFilmId(id);
        filmStore.delete(film);
    }

    @Transactional(readOnly = true)
    public Film getEntityById(Long id) {
        if (id == null) {
            throw new ValidationException("Film id must not be null");
        }
        return filmStore.findById(id)
                .orElseThrow(() -> new NotFoundException("Film with id " + id + " not found"));
    }

    private void validateRequest(FilmRequest request) {
        if (request == null) {
            throw new ValidationException("Film payload must not be null");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new ValidationException("Film title must not be blank");
        }
        if (request.getDurationMinutes() != null && request.getDurationMinutes() <= 0) {
            throw new ValidationException("Film duration must be positive");
        }
    }
}
