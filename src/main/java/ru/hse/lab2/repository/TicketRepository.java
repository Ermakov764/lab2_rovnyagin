package ru.hse.lab2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hse.lab2.entity.Ticket;
import java.time.LocalDate;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByFilmTitle(String filmTitle);

    List<Ticket> findByFilmTitleAndSessionDate(String filmTitle, LocalDate date);
    boolean existsByFilm_IdAndSessionDateAndSessionTimeAndSeatNumber(
            Long filmId, LocalDate sessionDate, java.time.LocalTime sessionTime, String seatNumber
    );
    boolean existsByFilm_IdAndSessionDateAndSessionTimeAndSeatNumberAndIdNot(
            Long filmId, LocalDate sessionDate, java.time.LocalTime sessionTime, String seatNumber, Long id
    );
    
    void deleteByFilm_Id(Long filmId);
    
    void deleteByViewer_Id(Long viewerId);

    @Query("""
        SELECT t.sessionDate, COUNT(DISTINCT t.viewer.id)
        FROM Ticket t
        WHERE t.film.id = :filmId
        GROUP BY t.sessionDate
        ORDER BY COUNT(DISTINCT t.viewer.id) DESC, t.sessionDate ASC
        """)
    List<Object[]> findDailyViewerStatsByFilmId(@Param("filmId") Long filmId);

    @Query("""
        SELECT t.film.id, t.film.title, COUNT(DISTINCT t.viewer.id)
        FROM Ticket t
        WHERE t.sessionDate = :date
        GROUP BY t.film.id, t.film.title
        ORDER BY COUNT(DISTINCT t.viewer.id) DESC, t.film.id ASC
        """)
    List<Object[]> findTopFilmByDate(@Param("date") LocalDate date);
}