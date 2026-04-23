#!/usr/bin/env python3
"""
LAB5: сидирование и точечная очистка Cinema REST (requests + Faker).

Сидирование:
  python3 tools/seed_rest_data.py --endpoint films --count 50
  python3 tools/seed_rest_data.py --endpoint viewers --count 100
  python3 tools/seed_rest_data.py --endpoint tickets --count 100 --divisor 10
    → создаёт max(1, 100//10)=10 фильмов и 10 зрителей, затем 100 билетов (только clear/tickets перед этим).
  python3 tools/seed_rest_data.py --endpoint all --count 200

Только очистка (без --count):
  python3 tools/seed_rest_data.py --endpoint films --clear    # билеты + фильмы
  python3 tools/seed_rest_data.py --endpoint viewers --clear  # билеты + зрители
  python3 tools/seed_rest_data.py --endpoint tickets --clear  # билеты + зрители (как viewers)
  python3 tools/seed_rest_data.py --endpoint all --clear      # всё
"""
from __future__ import annotations

import argparse
import random
import sys
import uuid
from datetime import date, timedelta, time
from typing import Any, List

import requests
from faker import Faker

fake = Faker("ru_RU")
Faker.seed(42)
random.seed(42)

# Какой path передать в DELETE /api/admin/clear/{resource}
CLEAR_API_PATH = {
    "films": "films",  # tickets + films; зрители остаются
    "viewers": "viewers",  # tickets + viewers; фильмы остаются
    "tickets": "viewers",  # по ТЗ: очистить билеты и зрителей → тот же TRUNCATE, что и viewers
    "all": "all",
}


def die(msg: str, code: int = 1) -> None:
    print(msg, file=sys.stderr)
    raise SystemExit(code)


def req_clear(session: requests.Session, base: str, resource: str) -> None:
    # Формируем URL для удаления ресурса (например, фильмов, зрителей или всего)
    url = f"{base}/api/admin/clear/{resource}"
    # Выполняем DELETE-запрос с увеличенным таймаутом (120 секунд)
    r = session.delete(url, timeout=120)
    # Проверяем, что код ответа — 204 No Content; иначе — аварийно завершаем работу
    if r.status_code != 204:
        die(f"DELETE {url} -> {r.status_code}: {r.text[:500]}")


def post_json(session: requests.Session, url: str, payload: dict[str, Any]) -> requests.Response:
    # Выполняет POST-запрос с передачей payload в формате JSON и стандартным заголовком Content-Type
    return session.post(url, json=payload, headers={"Content-Type": "application/json"}, timeout=60)


def get_json(session: requests.Session, url: str) -> Any:
    # Выполняет GET-запрос по указанному url с таймаутом 60 секунд
    r = session.get(url, timeout=60)
    # Если ответ не OK (200) — аварийно завершаем работу с сообщением об ошибке
    if r.status_code != 200:
        die(f"GET {url} -> {r.status_code}: {r.text[:500]}")
    # Возвращаем декодированный JSON-ответ
    return r.json()


def _seed_films_only(session: requests.Session, base: str, count: int) -> None:
    # Формируем URL для добавления фильмов через API
    url = f"{base}/api/films"
    # Создаем указанное количество фильмов
    for i in range(count):
        # Генерируем уникальное название фильма (4 случайных слова + индекс + часть uuid)
        title = f"{fake.sentence(nb_words=4).strip('.')}_{i}_{uuid.uuid4().hex[:8]}"
        # Формируем JSON-тело запроса (название, жанр, продолжительность)
        body = {
            "title": title[:255],  # обрезаем до 255 символов
            "genre": (fake.word() or "Drama")[:100],  # случайный жанр, не длиннее 100 символов
            "durationMinutes": random.randint(60, 200),  # продолжительность в минутах
        }
        # Выполняем POST-запрос для добавления фильма
        r = post_json(session, url, body)
        # Если не вернулся статус 201 — сообщаем об ошибке и аварийно выходим
        if r.status_code != 201:
            die(f"POST film {i} -> {r.status_code}: {r.text[:300]}")
        # Каждые 100 фильмов выводим прогресс в консоль
        if (i + 1) % 100 == 0:
            print(f"  films: {i + 1}/{count}")


def _seed_viewers_only(session: requests.Session, base: str, count: int) -> None:
    # Формируем URL для добавления зрителей
    url = f"{base}/api/viewers"
    # Создаем указанное количество зрителей
    for i in range(count):
        # Формируем уникальный email: user_<uuid>_<индекс>@<случайный_домен>
        email = f"user_{uuid.uuid4().hex[:12]}_{i}@{fake.domain_name()}"
        # Формируем JSON-тело запроса (имя и email)
        body = {"name": (fake.name() or "User")[:255], "email": email[:255]}
        # Выполняем POST-запрос для добавления зрителя
        r = post_json(session, url, body)
        # Если не вернулся статус 201 — сообщаем об ошибке и аварийно выходим
        if r.status_code != 201:
            die(f"POST viewer {i} -> {r.status_code}: {r.text[:300]}")
        # Каждые 100 зрителей выводим прогресс в консоль
        if (i + 1) % 100 == 0:
            print(f"  viewers: {i + 1}/{count}")


def _film_viewer_ids(session: requests.Session, base: str) -> tuple[List[int], List[int]]:
    # Получаем все фильмы из API (GET-запрос)
    films = get_json(session, f"{base}/api/films")
    # Получаем всех зрителей из API (GET-запрос)
    viewers = get_json(session, f"{base}/api/viewers")
    # Извлекаем id фильмов (как int)
    f_ids = [int(f["id"]) for f in films]
    # Извлекаем id зрителей (как int)
    v_ids = [int(v["id"]) for v in viewers]
    # Возвращаем два списка — id фильмов и id зрителей
    return f_ids, v_ids


def _seed_tickets_only(session: requests.Session, base: str, count: int) -> None:
    # Получаем id всех фильмов и зрителей (для назначения билетов)
    f_ids, v_ids = _film_viewer_ids(session, base)
    # Если список фильмов или зрителей пуст — сообщаем об ошибке и завершаем работу
    if not f_ids or not v_ids:
        die("Нет фильмов или зрителей для билетов.")
    # URL для добавления билетов
    url = f"{base}/api/tickets"
    # Запоминаем сегодняшнюю дату как базовую для генерации дат сессий
    base_d = date.today()
    # Генерируем необходимое количество билетов
    for i in range(count):
        # Случайным образом выбираем фильм и зрителя
        film_id = random.choice(f_ids)
        viewer_id = random.choice(v_ids)
        # Вычисляем дату (циклически +0...19 дней к текущей дате)
        d = base_d + timedelta(days=(i % 20))
        # Вычисляем время: часы 10...17, минуты меняются по формуле, секунды — 0
        t = time(hour=10 + (i % 8), minute=(i * 13) % 60, second=0)
        # Генерируем уникальный номер места из буквы + номер ряда + часть uuid
        seat = f"{(i % 26)}{i // 26}-{uuid.uuid4().hex[:4]}"
        # Формируем JSON-тело с данными билета
        body = {
            "viewerId": viewer_id,  # id зрителя
            "filmId": film_id,      # id фильма
            "sessionDate": d.isoformat(),  # дата сеанса в формате ISO
            "sessionTime": t.isoformat(timespec="seconds"),  # время сеанса в формате ISO (часы:минуты:секунды)
            "seatNumber": seat[:50],     # номер места, не больше 50 символов
            "price": round(random.uniform(200, 900), 2),  # цена с двумя знаками после запятой
        }
        r = post_json(session, url, body)
        if r.status_code != 201:
            if r.status_code == 409:
                continue
            die(f"POST ticket {i} -> {r.status_code}: {r.text[:300]}")
        if (i + 1) % 100 == 0:
            print(f"  tickets: {i + 1}/{count}")


def run_clear_only(session: requests.Session, base: str, endpoint: str) -> None:
    path = CLEAR_API_PATH[endpoint]
    print(f"  DELETE /api/admin/clear/{path} …")
    req_clear(session, base, path)
    print("  Очистка выполнена (204).")


def seed_films(session: requests.Session, base: str, count: int) -> None:
    req_clear(session, base, "films")
    _seed_films_only(session, base, count)


def seed_viewers(session: requests.Session, base: str, count: int) -> None:
    req_clear(session, base, "viewers")
    _seed_viewers_only(session, base, count)


def seed_tickets(session: requests.Session, base: str, count: int, divisor: int) -> None:
    if divisor < 1:
        die("--divisor must be >= 1")
    per_side = max(1, count // divisor)
    print(f"  билеты: count={count}, divisor={divisor} → добавляю по {per_side} фильмов и зрителей, затем {count} билетов")
    req_clear(session, base, "tickets")
    _seed_films_only(session, base, per_side)
    _seed_viewers_only(session, base, per_side)
    _seed_tickets_only(session, base, count)


def seed_all(session: requests.Session, base: str, count: int) -> None:
    req_clear(session, base, "all")
    print("  создаю films...")
    _seed_films_only(session, base, count)
    print("  создаю viewers...")
    _seed_viewers_only(session, base, count)
    print("  создаю tickets...")
    _seed_tickets_only(session, base, count)


def main() -> None:
    p = argparse.ArgumentParser(description="LAB5: сидирование / очистка Cinema REST")
    p.add_argument("--base-url", default="http://localhost:8080", help="Базовый URL API")
    p.add_argument(
        "--endpoint",
        required=True,
        choices=["films", "viewers", "tickets", "all"],
        help="Ресурс",
    )
    p.add_argument(
        "--count",
        type=int,
        default=None,
        help="Число объектов (для all — по каждому типу). По умолчанию 500, не нужен при --clear",
    )
    p.add_argument(
        "--clear",
        action="store_true",
        help="Только очистить таблицы по правилам endpoint (без сидирования)",
    )
    p.add_argument(
        "--divisor",
        type=int,
        default=10,
        help="Для --endpoint tickets: число фильмов и зрителей = max(1, count // divisor)",
    )
    args = p.parse_args()
    base = args.base_url.rstrip("/")
    session = requests.Session()

    if args.clear:
        print(f"BASE={base} endpoint={args.endpoint} mode=CLEAR")
        run_clear_only(session, base, args.endpoint)
        return

    n = args.count if args.count is not None else 500
    if n < 1:
        die("--count must be >= 1")

    print(f"BASE={base} endpoint={args.endpoint} count={n}")
    if args.endpoint == "films":
        seed_films(session, base, n)
    elif args.endpoint == "viewers":
        seed_viewers(session, base, n)
    elif args.endpoint == "tickets":
        seed_tickets(session, base, n, args.divisor)
    else:
        seed_all(session, base, n)
    print("Готово.")


if __name__ == "__main__":
    main()
