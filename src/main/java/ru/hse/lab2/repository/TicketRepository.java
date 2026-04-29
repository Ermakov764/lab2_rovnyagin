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

    /**
     * PostgreSQL: по каждому фильму с билетами — день с макс. числом уникальных зрителей
     * и это число (как один ряд после «сведения», без миллиона строк «фильм×день» в JVM).
     */
    @Query(value = """
        WITH daily AS (
            SELECT f.id AS fid,
                   f.title AS ftitle,
                   t.session_date AS d,
                   COUNT(DISTINCT t.viewer_id) AS vc
            FROM tickets t
            INNER JOIN films f ON f.id = t.film_id
            GROUP BY f.id, f.title, t.session_date
        ),
        ranked AS (
            SELECT fid, ftitle, d, vc,
                   ROW_NUMBER() OVER (PARTITION BY fid ORDER BY vc DESC, d ASC) AS rn
            FROM daily
        )
        SELECT fid, ftitle, d, vc
        FROM ranked
        WHERE rn = 1
        ORDER BY fid
        """, nativeQuery = true)
    List<Object[]> findAllFilmDailyViewerAggregates();
}