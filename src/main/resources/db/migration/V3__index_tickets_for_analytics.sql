-- Ускорение аналитики по film_id (в т. ч. LATERAL / EXISTS), иначе на больших tickets SELECT DISTINCT / GROUP BY валит планировщик и TCP.
CREATE INDEX IF NOT EXISTS idx_tickets_film_id ON tickets (film_id);
CREATE INDEX IF NOT EXISTS idx_tickets_film_session_date ON tickets (film_id, session_date);
