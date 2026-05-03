package ru.hse.lab2.api.dto;

import java.time.LocalDate;

/**
 * Одна календарная дата сеансов и число уникальных зрителей за этот день (по фильму).
 * Для internal API: Additional сам выбирает «лучший» день в Java.
 */
public record FilmDayStatDto(LocalDate day, long viewersCount) {}
