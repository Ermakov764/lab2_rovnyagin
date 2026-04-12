CREATE TABLE viewers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    CONSTRAINT uk_viewers_email UNIQUE (email)
);

CREATE TABLE films (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    genre VARCHAR(255),
    duration_minutes INTEGER
);

CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    viewer_id BIGINT NOT NULL,
    film_id BIGINT NOT NULL,
    session_date DATE NOT NULL,
    session_time TIME NOT NULL,
    seat_number VARCHAR(255) NOT NULL,
    price DOUBLE PRECISION,
    CONSTRAINT fk_tickets_viewer FOREIGN KEY (viewer_id) REFERENCES viewers (id),
    CONSTRAINT fk_tickets_film FOREIGN KEY (film_id) REFERENCES films (id),
    CONSTRAINT uk_tickets_film_session_seat UNIQUE (film_id, session_date, session_time, seat_number)
);
