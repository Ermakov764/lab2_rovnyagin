package ru.hse.lab2.service;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LAB5: очистка данных через TRUNCATE + RESTART IDENTITY (PostgreSQL),
 * чтобы после сида снова были id с 1 (удобно для k6 FILM_ID=1).
 */
@Service
@Profile("!inmemory")
public class AdminMaintenanceService {

    private final JdbcTemplate jdbcTemplate;

    public AdminMaintenanceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void clearTickets() {
        jdbcTemplate.execute("TRUNCATE TABLE tickets RESTART IDENTITY CASCADE");
    }

    @Transactional
    public void clearFilms() {
        jdbcTemplate.execute("TRUNCATE TABLE tickets, films RESTART IDENTITY CASCADE");
    }

    @Transactional
    public void clearViewers() {
        jdbcTemplate.execute("TRUNCATE TABLE tickets, viewers RESTART IDENTITY CASCADE");
    }

    @Transactional
    public void clearAll() {
        jdbcTemplate.execute("TRUNCATE TABLE tickets, films, viewers RESTART IDENTITY CASCADE");
    }
}
