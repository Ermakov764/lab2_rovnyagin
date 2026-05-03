package ru.hse.lab8.additional.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Минимальный снимок строки каталога {@code GET /api/films}: остальные поля JSON от основного CRUD не нужны аналитике.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CrudFilm(Long id, String title) {
}
