package ru.hse.lab2.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.hse.lab2.service.AdminMaintenanceService;

/**
 * LAB5: эндпоинты очистки таблиц перед сидированием из внешнего скрипта.
 * В продакшене должны быть защищены; для учебного стенда — без авторизации.
 */
@RestController
@RequestMapping("/api/admin")
@Profile("!inmemory")
public class AdminMaintenanceController {

    private final AdminMaintenanceService adminMaintenanceService;

    public AdminMaintenanceController(AdminMaintenanceService adminMaintenanceService) {
        this.adminMaintenanceService = adminMaintenanceService;
    }

    @DeleteMapping("/clear/tickets")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearTickets() {
        adminMaintenanceService.clearTickets();
    }

    @DeleteMapping("/clear/films")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearFilms() {
        adminMaintenanceService.clearFilms();
    }

    @DeleteMapping("/clear/viewers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearViewers() {
        adminMaintenanceService.clearViewers();
    }

    @DeleteMapping("/clear/all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearAll() {
        adminMaintenanceService.clearAll();
    }
}
