package ru.hse.lab2.controller;

import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.lab2.api.dto.MaxViewersPerDayDto;
import ru.hse.lab2.api.dto.TicketDto;
import ru.hse.lab2.api.dto.TopFilmByDayDto;
import ru.hse.lab2.api.request.TicketRequest;
import ru.hse.lab2.service.TicketService;

import java.time.LocalDate;
import java.util.List;

/**
 * Ресурс билетов под {@code /api/tickets}.
 *
 * <p><b>Важно для лаб. 8 (микросервис Additional):</b>
 * <ul>
 *   <li>{@code GET /api/tickets} — все билеты (как раньше).</li>
 *   <li>{@code GET /api/tickets?filmId=x} — только билеты фильма {@code x}; «простой» фильтр без join-аналитики.</li>
 * </ul>
 * <p>Микросервис Additional для отчётов вызывает {@code GET /api/tickets} без параметров и строит {@code Map<filmId, билеты>} в памяти.</p>
 * Отдельно остаются «тяжёлые» пути {@code /api/tickets/analytics/...} у этого же контроллера — это не то, что зовёт Additional.
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketRestController {

    private final TicketService ticketService;

    public TicketRestController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /** Билеты; при {@code filmId} — только по фильму (простой CRUD-фильтр для микросервиса, без готовой аналитики). */
    @GetMapping
    public List<TicketDto> getAll(@RequestParam(required = false) Long filmId) {
        if (filmId == null) {
            return ticketService.getAll();
        }
        return ticketService.getAllByFilmId(filmId);
    }

    @GetMapping("/{id}")
    public TicketDto getById(@PathVariable Long id) {
        return ticketService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDto create(@RequestBody TicketRequest request) {
        return ticketService.create(request);
    }

    @PutMapping("/{id}")
    public TicketDto update(@PathVariable Long id, @RequestBody TicketRequest request) {
        return ticketService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        ticketService.delete(id);
    }

    @GetMapping("/analytics/max-viewers")
    public MaxViewersPerDayDto getMaxViewersPerDay(@RequestParam Long filmId) {
        return ticketService.getMaxViewersPerDayByFilm(filmId);
    }

    @GetMapping("/analytics/top-film-by-day")
    public List<TopFilmByDayDto> getTopFilmByDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ticketService.getTopFilmByDate(date);
    }
}
