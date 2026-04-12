package ru.hse.lab2.api.mapper;

import ru.hse.lab2.api.dto.FilmDto;
import ru.hse.lab2.api.request.FilmRequest;
import ru.hse.lab2.entity.Film;

public final class FilmMapper {

    private FilmMapper() {
    }

    public static FilmDto toDto(Film film) {
        return new FilmDto(
                film.getId(),
                film.getTitle(),
                film.getGenre(),
                film.getDurationMinutes()
        );
    }

    public static Film toEntity(FilmRequest request) {
        return new Film(
                request.getTitle(),
                request.getGenre(),
                request.getDurationMinutes()
        );
    }

    public static void updateEntity(Film target, FilmRequest request) {
        target.setTitle(request.getTitle());
        target.setGenre(request.getGenre());
        target.setDurationMinutes(request.getDurationMinutes());
    }
}
