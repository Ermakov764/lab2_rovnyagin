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
     * PostgreSQL: только для первых {@code maxRows} фильмов по возрастанию id среди тех, у кого есть билеты,
     * считается «лучший день» (как в однофильмовой аналитике). Без полного прохода по всем билетам таблицы —
     * иначе CTE по всей базе делает запрос неподъёмным и рвёт TCP.
     */
    @Query(value = """
        SELECT f.id AS fid, f.title AS ftitle, stats.d AS d, stats.vc AS vc
        FROM (
            SELECT DISTINCT t.film_id AS film_id
            FROM tickets t
            ORDER BY t.film_id
            LIMIT :maxRows
        ) lim
        INNER JOIN films f ON f.id = lim.film_id
        INNER JOIN LATERAL (
            SELECT t.session_date AS d,
                   COUNT(DISTINCT t.viewer_id) AS vc
            FROM tickets t
            WHERE t.film_id = lim.film_id
            GROUP BY t.session_date
            ORDER BY COUNT(DISTINCT t.viewer_id) DESC, t.session_date ASC
            LIMIT 1
        ) stats ON true
        ORDER BY f.id
        """, nativeQuery = true)
    List<Object[]> findAllFilmDailyViewerAggregates(@Param("maxRows") int maxRows);
}