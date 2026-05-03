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

    List<Ticket> findByFilm_Id(Long filmId);

    /** Дневные агрегаты для {@code TicketService#getMaxViewersPerDayByFilm} (публичный GET /api/tickets/analytics/...). */
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
     * Internal legacy: один «лучший» день на фильм уже в SQL (LATERAL + LIMIT 1).
     * Сводка для лаб. 8 в Additional собирается без этого запроса — публичные / внутренние эндпоинты
     * «все дни по фильму» + список фильмов (см. Additional-сервис).
     * <p>PostgreSQL: первые {@code maxRows} фильмов по id — через {@code films} + EXISTS,
     * без DISTINCT по всей {@code tickets} (иначе seq scan/hash на огромной таблице).
     */
    @Query(value = """
        SELECT pick.fid, pick.ftitle, stats.d AS d, stats.vc AS vc
        FROM (
            SELECT f.id AS fid, f.title AS ftitle
            FROM films f
            WHERE EXISTS (SELECT 1 FROM tickets t WHERE t.film_id = f.id)
            ORDER BY f.id
            LIMIT :maxRows
        ) pick
        INNER JOIN LATERAL (
            SELECT t.session_date AS d,
                   COUNT(DISTINCT t.viewer_id) AS vc
            FROM tickets t
            WHERE t.film_id = pick.fid
            GROUP BY t.session_date
            ORDER BY COUNT(DISTINCT t.viewer_id) DESC, t.session_date ASC
            LIMIT 1
        ) stats ON true
        ORDER BY pick.fid
        """, nativeQuery = true)
    List<Object[]> findAllFilmDailyViewerAggregates(@Param("maxRows") int maxRows);
}