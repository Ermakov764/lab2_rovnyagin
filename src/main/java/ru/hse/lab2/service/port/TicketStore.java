package ru.hse.lab2.service.port;

import ru.hse.lab2.entity.Ticket;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface TicketStore {
    List<Ticket> findAll();

    Optional<Ticket> findById(Long id);

    Ticket save(Ticket ticket);

    void delete(Ticket ticket);
    
    void deleteByFilmId(Long filmId);
    
    void deleteByViewerId(Long viewerId);

    boolean existsByFilmSessionAndSeat(Long filmId, LocalDate sessionDate, LocalTime sessionTime, String seatNumber);

    boolean existsByFilmSessionAndSeatAndIdNot(
            Long filmId,
            LocalDate sessionDate,
            LocalTime sessionTime,
            String seatNumber,
            Long id
    );

    List<Object[]> findDailyViewerStatsByFilmId(Long filmId);

    List<Object[]> findTopFilmByDate(LocalDate date);
}
