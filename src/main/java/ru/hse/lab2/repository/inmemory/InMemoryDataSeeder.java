package ru.hse.lab2.repository.inmemory;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.hse.lab2.entity.Film;
import ru.hse.lab2.entity.Ticket;
import ru.hse.lab2.entity.Viewer;
import ru.hse.lab2.service.port.FilmStore;
import ru.hse.lab2.service.port.TicketStore;
import ru.hse.lab2.service.port.ViewerStore;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
@Profile("inmemory")
public class InMemoryDataSeeder {

    private final FilmStore filmStore;
    private final ViewerStore viewerStore;
    private final TicketStore ticketStore;

    public InMemoryDataSeeder(FilmStore filmStore, ViewerStore viewerStore, TicketStore ticketStore) {
        this.filmStore = filmStore;
        this.viewerStore = viewerStore;
        this.ticketStore = ticketStore;
    }

    @PostConstruct
    public void seed() {
        if (!filmStore.findAll().isEmpty() || !viewerStore.findAll().isEmpty() || !ticketStore.findAll().isEmpty()) {
            return;
        }

        Film interstellar = filmStore.save(new Film("Interstellar", "Sci-Fi", 169));
        Film inception = filmStore.save(new Film("Inception", "Sci-Fi", 148));

        Viewer ivan = viewerStore.save(new Viewer("Ivan Petrov", "ivan@example.com"));
        Viewer anna = viewerStore.save(new Viewer("Anna Smirnova", "anna@example.com"));

        ticketStore.save(new Ticket(
                ivan,
                interstellar,
                LocalDate.of(2026, 4, 20),
                LocalTime.of(19, 0),
                "A1",
                500.0
        ));
        ticketStore.save(new Ticket(
                anna,
                interstellar,
                LocalDate.of(2026, 4, 20),
                LocalTime.of(19, 0),
                "A2",
                500.0
        ));
        ticketStore.save(new Ticket(
                anna,
                inception,
                LocalDate.of(2026, 4, 21),
                LocalTime.of(20, 30),
                "B3",
                450.0
        ));
    }
}
