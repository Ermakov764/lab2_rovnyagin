package ru.hse.lab2.repository.inmemory;

import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import ru.hse.lab2.debug.DebugProbe;
import ru.hse.lab2.entity.Ticket;
import ru.hse.lab2.service.port.TicketStore;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Profile("inmemory")
public class InMemoryTicketStore implements TicketStore {

    private final Map<Long, Ticket> tickets = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public synchronized List<Ticket> findAll() {
        return new ArrayList<>(tickets.values());
    }

    @Override
    public synchronized Optional<Ticket> findById(Long id) {
        return Optional.ofNullable(tickets.get(id));
    }

    @Override
    public synchronized Ticket save(Ticket ticket) {
        boolean duplicateSeat = tickets.values().stream().anyMatch(it ->
                !Objects.equals(it.getId(), ticket.getId())
                        && Objects.equals(it.getFilm().getId(), ticket.getFilm().getId())
                        && Objects.equals(it.getSessionDate(), ticket.getSessionDate())
                        && Objects.equals(it.getSessionTime(), ticket.getSessionTime())
                        && Objects.equals(it.getSeatNumber(), ticket.getSeatNumber())
        );
        if (duplicateSeat) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("filmId", ticket.getFilm() == null ? null : ticket.getFilm().getId());
            data.put("sessionDate", ticket.getSessionDate() == null ? null : ticket.getSessionDate().toString());
            data.put("sessionTime", ticket.getSessionTime() == null ? null : ticket.getSessionTime().toString());
            data.put("seatNumber", ticket.getSeatNumber());
            // #region agent log
            DebugProbe.log(
                    "run-1",
                    "H4",
                    "InMemoryTicketStore#save",
                    "Duplicate seat rejected in inmemory store",
                    data
            );
            // #endregion
            throw new DataIntegrityViolationException(
                    "Ticket with the same film/session/seat already exists"
            );
        }
        if (ticket.getId() == null) {
            ticket.setId(sequence.incrementAndGet());
        }
        tickets.put(ticket.getId(), ticket);
        return ticket;
    }

    @Override
    public synchronized void delete(Ticket ticket) {
        tickets.remove(ticket.getId());
    }
    
    @Override
    public synchronized void deleteByFilmId(Long filmId) {
        tickets.entrySet().removeIf(it -> Objects.equals(it.getValue().getFilm().getId(), filmId));
    }
    
    @Override
    public synchronized void deleteByViewerId(Long viewerId) {
        tickets.entrySet().removeIf(it -> Objects.equals(it.getValue().getViewer().getId(), viewerId));
    }

    @Override
    public synchronized boolean existsByFilmSessionAndSeat(Long filmId, LocalDate sessionDate, LocalTime sessionTime, String seatNumber) {
        return tickets.values().stream().anyMatch(it ->
                Objects.equals(it.getFilm().getId(), filmId)
                        && Objects.equals(it.getSessionDate(), sessionDate)
                        && Objects.equals(it.getSessionTime(), sessionTime)
                        && Objects.equals(it.getSeatNumber(), seatNumber)
        );
    }

    @Override
    public synchronized boolean existsByFilmSessionAndSeatAndIdNot(
            Long filmId,
            LocalDate sessionDate,
            LocalTime sessionTime,
            String seatNumber,
            Long id
    ) {
        return tickets.values().stream().anyMatch(it ->
                !Objects.equals(it.getId(), id)
                        && Objects.equals(it.getFilm().getId(), filmId)
                        && Objects.equals(it.getSessionDate(), sessionDate)
                        && Objects.equals(it.getSessionTime(), sessionTime)
                        && Objects.equals(it.getSeatNumber(), seatNumber)
        );
    }

    @Override
    public synchronized List<Object[]> findDailyViewerStatsByFilmId(Long filmId) {
        Map<LocalDate, Set<Long>> grouped = new LinkedHashMap<>();
        tickets.values().stream()
                .filter(it -> Objects.equals(it.getFilm().getId(), filmId))
                .forEach(it -> {
                    LocalDate date = it.getSessionDate();
                    grouped.computeIfAbsent(date, ignored -> ConcurrentHashMap.newKeySet())
                            .add(it.getViewer().getId());
                });

        List<Object[]> result = grouped.entrySet().stream()
                .sorted(
                        Comparator.<Map.Entry<LocalDate, Set<Long>>>comparingLong(it -> it.getValue().size())
                                .reversed()
                                .thenComparing(Map.Entry.comparingByKey())
                )
                .map(it -> new Object[]{it.getKey(), it.getValue().size()})
                .toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("filmId", filmId);
        data.put("daysCount", result.size());
        data.put("topDay", result.isEmpty() ? null : String.valueOf(result.getFirst()[0]));
        data.put("topCount", result.isEmpty() ? null : String.valueOf(result.getFirst()[1]));
        // #region agent log
        DebugProbe.log(
                "run-1",
                "H2",
                "InMemoryTicketStore#findDailyViewerStatsByFilmId",
                "Inmemory analytics calculated",
                data
        );
        // #endregion
        return result;
    }

    @Override
    public synchronized List<Object[]> findTopFilmByDate(LocalDate date) {
        Map<Long, Map.Entry<String, Set<Long>>> grouped = new LinkedHashMap<>();
        tickets.values().stream()
                .filter(it -> Objects.equals(it.getSessionDate(), date))
                .forEach(it -> grouped.compute(
                        it.getFilm().getId(),
                        (filmId, current) -> {
                            if (current == null) {
                                Set<Long> viewers = ConcurrentHashMap.newKeySet();
                                viewers.add(it.getViewer().getId());
                                return Map.entry(it.getFilm().getTitle(), viewers);
                            }
                            current.getValue().add(it.getViewer().getId());
                            return current;
                        }
                ));

        return grouped.entrySet().stream()
                .sorted(
                        Comparator.<Map.Entry<Long, Map.Entry<String, Set<Long>>>>comparingLong(
                                        it -> it.getValue().getValue().size()
                                )
                                .reversed()
                                .thenComparing(Map.Entry.comparingByKey())
                )
                .map(it -> new Object[]{
                        it.getKey(),
                        it.getValue().getKey(),
                        it.getValue().getValue().size()
                })
                .toList();
    }
}
