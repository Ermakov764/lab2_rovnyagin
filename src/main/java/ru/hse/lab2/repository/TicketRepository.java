package ru.hse.lab2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.hse.lab2.entity.Ticket;
import java.time.LocalDate;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Найти все билеты на фильм по названию
    List<Ticket> findByFilmTitle(String filmTitle);

    // Найти билеты на фильм в конкретную дату
    List<Ticket> findByFilmTitleAndSessionDate(String filmTitle, LocalDate date);

    // ГЛАВНЫЙ ЗАПРОС: макс. количество зрителей на фильме за день
    @Query("""
        SELECT t.sessionDate, COUNT(t) 
        FROM Ticket t 
        WHERE t.film.title = :filmTitle 
        GROUP BY t.sessionDate 
        ORDER BY COUNT(t) DESC
        """)
    List<Object[]> findMaxViewersPerDayByFilmTitle(@Param("filmTitle") String filmTitle);
}