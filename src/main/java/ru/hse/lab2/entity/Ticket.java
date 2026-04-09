package ru.hse.lab2.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "tickets")  // ← Исправлено: tickets
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Связь со зрителем (исправлено: viewer_id)
    @ManyToOne
    @JoinColumn(name = "viewer_id", nullable = false)
    private Viewer viewer;

    // 🔗 Связь с фильмом (НОВОЕ!)
    @ManyToOne
    @JoinColumn(name = "film_id", nullable = false)
    private Film film;

    // 🔥 Поля билета (вместо phone number)
    @Column(nullable = false)
    private LocalDate sessionDate;  // дата сеанса

    @Column(nullable = false)
    private LocalTime sessionTime;  // время сеанса

    @Column(nullable = false)
    private String seatNumber;      // номер места: "A12", "B5"

    private Double price;           // цена билета

    // Конструкторы
    public Ticket() {}

    public Ticket(Viewer viewer, Film film, LocalDate date, LocalTime time, String seat, Double price) {
        this.viewer = viewer;
        this.film = film;
        this.sessionDate = date;
        this.sessionTime = time;
        this.seatNumber = seat;
        this.price = price;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Viewer getViewer() { return viewer; }  // ← Было getUser()
    public void setViewer(Viewer viewer) { this.viewer = viewer; }

    public Film getFilm() { return film; }
    public void setFilm(Film film) { this.film = film; }

    public LocalDate getSessionDate() { return sessionDate; }
    public void setSessionDate(LocalDate sessionDate) { this.sessionDate = sessionDate; }

    public LocalTime getSessionTime() { return sessionTime; }
    public void setSessionTime(LocalTime sessionTime) { this.sessionTime = sessionTime; }

    public String getSeatNumber() { return seatNumber; }  // ← Было getNumber()
    public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}