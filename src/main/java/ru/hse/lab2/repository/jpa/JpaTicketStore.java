package ru.hse.lab2.repository.jpa;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.hse.lab2.entity.Ticket;
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

    public JpaTicketStore(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    public List<Ticket> findAll() {
        return ticketRepository.findAll();
    }

    @Override
    public Optional<Ticket> findById(Long id) {
        return ticketRepository.findById(id);
    }

    @Override
    public Ticket save(Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    @Override
    public void delete(Ticket ticket) {
        ticketRepository.delete(ticket);
    }
    
    @Override
    public void deleteByFilmId(Long filmId) {
        ticketRepository.deleteByFilm_Id(filmId);
    }
    
    @Override
    public void deleteByViewerId(Long viewerId) {
        ticketRepository.deleteByViewer_Id(viewerId);
    }

    @Override
    public boolean existsByFilmSessionAndSeat(Long filmId, LocalDate sessionDate, LocalTime sessionTime, String seatNumber) {
        return ticketRepository.existsByFilm_IdAndSessionDateAndSessionTimeAndSeatNumber(
                filmId, sessionDate, sessionTime, seatNumber
        );
    }

    @Override
    public boolean existsByFilmSessionAndSeatAndIdNot(
            Long filmId,
            LocalDate sessionDate,
            LocalTime sessionTime,
            String seatNumber,
            Long id
    ) {
        return ticketRepository.existsByFilm_IdAndSessionDateAndSessionTimeAndSeatNumberAndIdNot(
                filmId, sessionDate, sessionTime, seatNumber, id
        );
    }

    @Override
    public List<Object[]> findDailyViewerStatsByFilmId(Long filmId) {
        return ticketRepository.findDailyViewerStatsByFilmId(filmId);
    }

    @Override
    public List<Object[]> findTopFilmByDate(LocalDate date) {
        return ticketRepository.findTopFilmByDate(date);
    }
}
