package ru.hse.lab2.entity;

import jakarta.persistence.*;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "viewers")  // ← Исправлено: viewers
public class Viewer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;  // ← Исправлено: одно поле name

    @Column(nullable = false, unique = true)
    private String email;

    // ← Исправлено: mappedBy = "viewer" и List<Ticket>
    @OneToMany(mappedBy = "viewer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Ticket> tickets = new ArrayList<>();

    // Пустой конструктор
    public Viewer() {}

    // Конструктор с аргументами (теперь 2 параметра)
    public Viewer(String name, String email) {
        this.name = name;
        this.email = email;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }  // ← Было getFirstName
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public List<Ticket> getTickets() { return tickets; }  // ← Было getPhones
    public void setTickets(List<Ticket> tickets) { this.tickets = tickets; }
}