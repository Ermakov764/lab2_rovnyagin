package ru.hse.lab2.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.hse.lab2.api.dto.MaxViewersPerDayDto;
import ru.hse.lab2.api.dto.TicketDto;
import ru.hse.lab2.api.dto.TopFilmByDayDto;
import ru.hse.lab2.api.mapper.TicketMapper;
import ru.hse.lab2.api.request.TicketRequest;
import ru.hse.lab2.entity.Film;
import ru.hse.lab2.entity.Ticket;
import ru.hse.lab2.entity.Viewer;
import ru.hse.lab2.exception.ConflictException;
import ru.hse.lab2.exception.NotFoundException;
import ru.hse.lab2.exception.ValidationException;
import ru.hse.lab2.debug.DebugProbe;
import ru.hse.lab2.service.port.TicketStore;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;

@Service
public class TicketService {

    private final TicketStore ticketStore;
    private final ViewerService viewerService;
    private final FilmService filmService;

    public TicketService(
            TicketStore ticketStore,
            ViewerService viewerService,
            FilmService filmService
    ) {
        this.ticketStore = ticketStore;
        this.viewerService = viewerService;
        this.filmService = filmService;
    }

    @Transactional(readOnly = true)
    public List<TicketDto> getAll() {
        return ticketStore.findAll().stream()
                .map(TicketMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketDto getById(Long id) {
        return TicketMapper.toDto(getEntityById(id));
    }

    @Transactional
    public TicketDto create(TicketRequest request) {
        validateRequest(request);
        String normalizedSeatNumber = normalizeSeatNumber(request.getSeatNumber());
        LinkedHashMap<String, Object> createPayload = new LinkedHashMap<>();
        createPayload.put("filmId", request.getFilmId());
        createPayload.put("viewerId", request.getViewerId());
        createPayload.put("sessionDate", request.getSessionDate() == null ? null : request.getSessionDate().toString());
        createPayload.put("seatNumberRaw", request.getSeatNumber());
        createPayload.put("seatNumberNormalized", normalizedSeatNumber);
        // #region agent log
        DebugProbe.log(
                "run-1",
                "H3",
                "TicketService#create",
                "Ticket create request accepted",
                createPayload
        );
        // #endregion
        Viewer viewer = viewerService.getEntityById(request.getViewerId());
        Film film = filmService.getEntityById(request.getFilmId());
        ensureSeatIsAvailable(
                film.getId(),
                request.getSessionDate(),
                request.getSessionTime(),
                normalizedSeatNumber,
                null
        );

        Ticket ticket = TicketMapper.toEntity(request, viewer, film);
        ticket.setSeatNumber(normalizedSeatNumber);
        try {
            return TicketMapper.toDto(ticketStore.save(ticket));
        } catch (DataIntegrityViolationException e) {
            LinkedHashMap<String, Object> conflictPayload = new LinkedHashMap<>();
            conflictPayload.put("filmId", request.getFilmId());
            conflictPayload.put("viewerId", request.getViewerId());
            conflictPayload.put("sessionDate", request.getSessionDate() == null ? null : request.getSessionDate().toString());
            conflictPayload.put("seatNumberNormalized", normalizedSeatNumber);
            // #region agent log
            DebugProbe.log(
                    "run-1",
                    "H3",
                    "TicketService#create",
                    "Ticket create conflict from store",
                    conflictPayload
            );
            // #endregion
            throw new ConflictException("Ticket with the same film/session/seat already exists");
        }
    }

    @Transactional
    public TicketDto update(Long id, TicketRequest request) {
        validateRequest(request);
        String normalizedSeatNumber = normalizeSeatNumber(request.getSeatNumber());
        Ticket existing = getEntityById(id);
        Viewer viewer = viewerService.getEntityById(request.getViewerId());
        Film film = filmService.getEntityById(request.getFilmId());
        ensureSeatIsAvailable(
                film.getId(),
                request.getSessionDate(),
                request.getSessionTime(),
                normalizedSeatNumber,
                id
        );

        TicketMapper.updateEntity(existing, request, viewer, film);
        existing.setSeatNumber(normalizedSeatNumber);
        try {
            return TicketMapper.toDto(ticketStore.save(existing));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Ticket with the same film/session/seat already exists");
        }
    }

    @Transactional
    public void delete(Long id) {
        Ticket ticket = getEntityById(id);
        ticketStore.delete(ticket);
    }

    @Transactional(readOnly = true)
    public MaxViewersPerDayDto getMaxViewersPerDayByFilm(Long filmId) {
        Film film = filmService.getEntityById(filmId);
        List<Object[]> stats = ticketStore.findDailyViewerStatsByFilmId(filmId);
        if (stats.isEmpty()) {
            throw new NotFoundException("No tickets found for film with id " + filmId);
        }

        Object[] top = stats.getFirst();
        LocalDate date = (LocalDate) top[0];
        long viewersCount = ((Number) top[1]).longValue();
        return new MaxViewersPerDayDto(film.getId(), date, viewersCount);
    }

    @Transactional(readOnly = true)
    public List<TopFilmByDayDto> getTopFilmByDate(LocalDate date) {
        if (date == null) {
            throw new ValidationException("Date must not be null");
        }
        return ticketStore.findTopFilmByDate(date).stream()
                .map(row -> new TopFilmByDayDto(
                        ((Number) row[0]).longValue(),
                        String.valueOf(row[1]),
                        date,
                        ((Number) row[2]).longValue()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public Ticket getEntityById(Long id) {
        if (id == null) {
            throw new ValidationException("Ticket id must not be null");
        }
        return ticketStore.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket with id " + id + " not found"));
    }

    private void ensureSeatIsAvailable(
            Long filmId,
            LocalDate sessionDate,
            java.time.LocalTime sessionTime,
            String seatNumber,
            Long excludedTicketId
    ) {
        boolean conflict = excludedTicketId == null
                ? ticketStore.existsByFilmSessionAndSeat(
                filmId, sessionDate, sessionTime, seatNumber
        )
                : ticketStore.existsByFilmSessionAndSeatAndIdNot(
                filmId, sessionDate, sessionTime, seatNumber, excludedTicketId
        );

        if (conflict) {
            throw new ConflictException(
                    "Seat '" + seatNumber + "' is already taken for this film session"
            );
        }
    }

    private void validateRequest(TicketRequest request) {
        if (request == null) {
            throw new ValidationException("Ticket payload must not be null");
        }
        if (request.getViewerId() == null) {
            throw new ValidationException("Viewer id must not be null");
        }
        if (request.getFilmId() == null) {
            throw new ValidationException("Film id must not be null");
        }
        if (request.getSessionDate() == null) {
            throw new ValidationException("Session date must not be null");
        }
        if (request.getSessionTime() == null) {
            throw new ValidationException("Session time must not be null");
        }
        if (request.getSeatNumber() == null || request.getSeatNumber().trim().isEmpty()) {
            throw new ValidationException("Seat number must not be blank");
        }
        if (request.getPrice() != null && request.getPrice() < 0) {
            throw new ValidationException("Ticket price must be non-negative");
        }
    }

    private String normalizeSeatNumber(String seatNumber) {
        return seatNumber.trim();
    }
}
