package ru.hse.lab2.repository.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.hse.lab2.entity.Ticket;
import ru.hse.lab2.observability.ObservabilityService;
import ru.hse.lab2.repository.TicketRepository;
import ru.hse.lab2.service.port.TicketStore;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Component
@Profile("!inmemory")
public class JpaTicketStore implements TicketStore {

    private final TicketRepository ticketRepository;
    private final ObservabilityService observabilityService;

    public JpaTicketStore(TicketRepository ticketRepository, ObservabilityService observabilityService) {
        this.ticketRepository = ticketRepository;
        this.observabilityService = observabilityService;
    }

    @Override
    public List<Ticket> findAll() {
        return timed("db.JpaTicketStore.findAll", ticketRepository::findAll);
    }

    @Override
    public List<Ticket> findByFilmId(Long filmId) {
        return timed("db.JpaTicketStore.findByFilmId", () -> ticketRepository.findByFilm_Id(filmId));
    }

    @Override
    public Optional<Ticket> findById(Long id) {
        return timed("db.JpaTicketStore.findById", () -> ticketRepository.findById(id));
    }

    @Override
    public Ticket save(Ticket ticket) {
        return timed("db.JpaTicketStore.save", () -> ticketRepository.save(ticket));
    }

    @Override
    public void delete(Ticket ticket) {
        timedVoid("db.JpaTicketStore.delete", () -> ticketRepository.delete(ticket));
    }
    
    @Override
    public void deleteByFilmId(Long filmId) {
        timedVoid("db.JpaTicketStore.deleteByFilmId", () -> ticketRepository.deleteByFilm_Id(filmId));
    }
    
    @Override
    public void deleteByViewerId(Long viewerId) {
        timedVoid("db.JpaTicketStore.deleteByViewerId", () -> ticketRepository.deleteByViewer_Id(viewerId));
    }

    @Override
    public boolean existsByFilmSessionAndSeat(Long filmId, LocalDate sessionDate, LocalTime sessionTime, String seatNumber) {
        return timed("db.JpaTicketStore.existsByFilmSessionAndSeat",
                () -> ticketRepository.existsByFilm_IdAndSessionDateAndSessionTimeAndSeatNumber(
                        filmId, sessionDate, sessionTime, seatNumber
                ));
    }

    @Override
    public boolean existsByFilmSessionAndSeatAndIdNot(
            Long filmId,
            LocalDate sessionDate,
            LocalTime sessionTime,
            String seatNumber,
            Long id
    ) {
        return timed("db.JpaTicketStore.existsByFilmSessionAndSeatAndIdNot",
                () -> ticketRepository.existsByFilm_IdAndSessionDateAndSessionTimeAndSeatNumberAndIdNot(
                        filmId, sessionDate, sessionTime, seatNumber, id
                ));
    }

    @Override
    public List<Object[]> findDailyViewerStatsByFilmId(Long filmId) {
        return timed("db.JpaTicketStore.findDailyViewerStatsByFilmId",
                () -> ticketRepository.findDailyViewerStatsByFilmId(filmId));
    }

    @Override
    public List<Object[]> findTopFilmByDate(LocalDate date) {
        return timed("db.JpaTicketStore.findTopFilmByDate", () -> ticketRepository.findTopFilmByDate(date));
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
