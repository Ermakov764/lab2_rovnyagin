INSERT INTO films (id, title, genre, duration_minutes) VALUES
    (1, 'Интерстеллар', 'Фантастика', 169),
    (2, 'Начало', 'Фантастика', 148),
    (3, 'Зелёная миля', 'Драма', 189);

INSERT INTO viewers (id, name, email) VALUES
    (1, 'Алексей', 'alex@test.ru'),
    (2, 'Мария', 'maria@test.ru'),
    (3, 'Дмитрий', 'dima@test.ru'),
    (4, 'Елена', 'elena@test.ru');

INSERT INTO tickets (viewer_id, film_id, session_date, session_time, seat_number, price) VALUES
    (1, 1, '2026-04-20', '18:00:00', 'A1', 500.0),
    (2, 1, '2026-04-20', '18:00:00', 'A2', 500.0),
    (3, 1, '2026-04-20', '18:00:00', 'A3', 500.0),
    (4, 1, '2026-04-20', '21:00:00', 'B1', 500.0),
    (1, 2, '2026-04-21', '19:00:00', 'C1', 450.0),
    (2, 3, '2026-04-21', '20:00:00', 'D1', 400.0);

SELECT setval('films_id_seq', COALESCE((SELECT MAX(id) FROM films), 1), true);
SELECT setval('viewers_id_seq', COALESCE((SELECT MAX(id) FROM viewers), 1), true);
