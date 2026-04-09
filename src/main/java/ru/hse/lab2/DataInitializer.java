package ru.hse.lab2;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.hse.lab2.entity.Viewer;
import ru.hse.lab2.entity.Film;
import ru.hse.lab2.entity.Ticket;
import ru.hse.lab2.repository.ViewerRepository;
import ru.hse.lab2.repository.FilmRepository;
import ru.hse.lab2.repository.TicketRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ViewerRepository viewerRepository;
    private final FilmRepository filmRepository;
    private final TicketRepository ticketRepository;

    public DataInitializer(ViewerRepository viewerRepository,
                           FilmRepository filmRepository,
                           TicketRepository ticketRepository) {
        this.viewerRepository = viewerRepository;
        this.filmRepository = filmRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public void run(String... args) {
        if (filmRepository.count() == 0) {  // ← Проверяем по фильмам (главная сущность)

            //  Создаём фильмы
            Film film1 = new Film("Интерстеллар", "Фантастика", 169);
            Film film2 = new Film("Начало", "Фантастика", 148);
            Film film3 = new Film("Зелёная миля", "Драма", 189);
            filmRepository.saveAll(List.of(film1, film2, film3));

            // Создаём зрителей
            Viewer viewer1 = new Viewer("Алексей", "alex@test.ru");
            Viewer viewer2 = new Viewer("Мария", "maria@test.ru");
            Viewer viewer3 = new Viewer("Дмитрий", "dima@test.ru");
            Viewer viewer4 = new Viewer("Елена", "elena@test.ru");
            viewerRepository.saveAll(List.of(viewer1, viewer2, viewer3, viewer4));

            // Создаём билеты (сеансы)
            LocalDate date1 = LocalDate.of(2026, 4, 20);
            LocalDate date2 = LocalDate.of(2026, 4, 21);

            // Сеансы "Интерстеллара" 20 апреля (пик посещаемости!)
            ticketRepository.save(new Ticket(viewer1, film1, date1, LocalTime.of(18, 0), "A1", 500.0));
            ticketRepository.save(new Ticket(viewer2, film1, date1, LocalTime.of(18, 0), "A2", 500.0));
            ticketRepository.save(new Ticket(viewer3, film1, date1, LocalTime.of(18, 0), "A3", 500.0));

            // Вечерний сеанс того же фильма
            ticketRepository.save(new Ticket(viewer4, film1, date1, LocalTime.of(21, 0), "B1", 500.0));

            // Сеансы на другие фильмы (для разнообразия)
            ticketRepository.save(new Ticket(viewer1, film2, date2, LocalTime.of(19, 0), "C1", 450.0));
            ticketRepository.save(new Ticket(viewer2, film3, date2, LocalTime.of(20, 0), "D1", 400.0));

            System.out.println("Тестовые данные для кинотеатра успешно добавлены!");
            System.out.println("Фильмов: " + filmRepository.count());
            System.out.println("Зрителей: " + viewerRepository.count());
            System.out.println("Билетов: " + ticketRepository.count());

        } else {
            System.out.println("ℹ️ База данных уже содержит данные, пропускаем инициализацию.");
        }
    }
}