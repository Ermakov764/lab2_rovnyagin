package ru.hse.lab2.api.mapper;

import ru.hse.lab2.api.dto.TicketDto;
import ru.hse.lab2.api.request.TicketRequest;
import ru.hse.lab2.entity.Film;
import ru.hse.lab2.entity.Ticket;
import ru.hse.lab2.entity.Viewer;

public final class TicketMapper {

    private TicketMapper() {
    }

    public static TicketDto toDto(Ticket ticket) {
        return new TicketDto(
                ticket.getId(),
                ticket.getViewer().getId(),
                ticket.getFilm().getId(),
                ticket.getSessionDate(),
                ticket.getSessionTime(),
                ticket.getSeatNumber(),
                ticket.getPrice()
        );
    }

    public static Ticket toEntity(TicketRequest request, Viewer viewer, Film film) {
        return new Ticket(
                viewer,
                film,
                request.getSessionDate(),
                request.getSessionTime(),
                request.getSeatNumber(),
                request.getPrice()
        );
    }

    public static void updateEntity(Ticket target, TicketRequest request, Viewer viewer, Film film) {
        target.setViewer(viewer);
        target.setFilm(film);
        target.setSessionDate(request.getSessionDate());
        target.setSessionTime(request.getSessionTime());
        target.setSeatNumber(request.getSeatNumber());
        target.setPrice(request.getPrice());
    }
}
