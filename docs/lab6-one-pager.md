# Лабораторная 6 — одна страница

Полная версия: **`README.md`** (раздел *«Лабораторная работа №6»*), задание: **`ТЗ_6лаба.txt`**.

---

## Суть

ВМ → **Docker Compose** (app + Postgres + pgAdmin) → конфиг через **`.env`** (БД, Tomcat, JPA, лимиты CPU/RAM) → доступ с ПК по **SSH `-L 8080:localhost:8080`** → **k6** (const VU, смеси 5/95, 50/50, 95/5) → **графики** по CPU.

---

## Порты (не путать)

| Порт | Где | Что |
|------|-----|-----|
| **8080** | ВМ / туннель с ПК | HTTP приложения (HTML, REST, Swagger) |
| **5432** | ВМ | PostgreSQL с хоста |
| **15432** | ВМ | pgAdmin → `http://<хост>:15432` |
| **2303** *(пример)* | ПК → ВМ | SSH персональной ВМ (см. таблицу курса) |
| **2311** *(пример)* | hl03 → k6-ВМ | SSH общей машины с k6 |

---

## ВМ: подъём с нуля

```bash
cd ~/lab2_rovnyagin
cp -n .env.example .env
# .env: DOCKER_IMAGE_APP; POSTGRES_DB и SPRING_DATASOURCE_URL — одно имя БД (часто hl3)
docker compose down -v
docker compose pull app
docker compose up -d
docker compose ps
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/
```

Логи при падении: `docker compose logs app --tail 80`

---

## ПК: туннель к ВМ

Из корня репозитория:

```bash
./scripts/ssh-tunnel-personal-vm.sh
# другой SSH-порт: SSH_PORT=XXXX ./scripts/ssh-tunnel-personal-vm.sh
```

Браузер: `http://localhost:8080/`, Swagger: `http://localhost:8080/swagger-ui.html`  
Не держите одновременно локальный `lab2_app` на **8080** ПК и туннель.

---

## Переменные `.env` (главное)

| Переменная | Зачем |
|------------|--------|
| `DOCKER_IMAGE_APP` | Образ с Docker Hub |
| `POSTGRES_DB` + `SPRING_DATASOURCE_URL` | **Одинаковое имя БД** в обоих |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | Учётка БД |
| `SERVER_TOMCAT_THREADS_MAX` | Потоки Tomcat (= `server.tomcat.threads.max`) |
| `SPRING_JPA_SHOW_SQL` | `false` — тише лог (п. 9 ТЗ) |
| `APP_CPU_LIMIT` | Лимит CPU контейнера `app` (шаг 0.5 для серии) |

После смены CPU: `docker compose up -d --force-recreate app`

---

## k6 (п. 10)

Сценарий: **`k6/cinema-lab6-constant.js`**. Три смеси подряд: **`./k6/run-lab6-ratio-sweep.sh`**.

```bash
export TARGET_VUS=30
export BASE_URL=http://127.0.0.1:8080   # с ПК при открытом туннеле
# или BASE_URL=http://<IP_приложения>:8080 — с машины, откуда виден API
./k6/run-lab6-ratio-sweep.sh
```

Копирование отчётов по CPU: `RESULT_CPU=0.5|1.0|1.5|2 ./k6/run-lab6-ratio-sweep.sh`  
Графики: `python3 k6/plot_lab6_from_results.py results` (нужен matplotlib)

**ТЗ:** два прогона — **ПК → сервер** (туннель + k6 на ПК) и **сервер → сервер** (k6 там, где корректен `BASE_URL`).

---

## Частые ошибки

| Симптом | Действие |
|---------|----------|
| `database "hl3" does not exist` | Согласовать `.env`; при необходимости `docker compose down -v` и `up -d` |
| `address already in use :8080` | На ПК: туннель **или** локальный Docker — не оба на один порт |
| k6 падает на GET | Сиды / `FILM_ID`: `./tools/run-seed.sh`, проверить `GET /api/films` |

---

## Файлы лаб. 6

| Файл | Роль |
|------|------|
| `scripts/ssh-tunnel-personal-vm.sh` | Туннель 8080 с ПК на ВМ |
| `k6/cinema-lab6-constant.js` | constant VU, POST/GET |
| `k6/run-lab6-ratio-sweep.sh` | 5/95, 50/50, 95/5 подряд |
| `k6/plot_lab6_from_results.py` | PNG из `results/cpu-*` |
| `k6/remote-k6-sync-and-run.sh` | rsync k6 + запуск на удалённой k6-ВМ |

---

## Соответствие ТЗ (ультракратко)

1–3 ВМ, ключи, git, `docker login` — вручную. 4 `docker-compose.yml`. 5 туннель. 6 Swagger. 7 `deploy.resources`. 8 env JDBC + Tomcat. 9 `SPRING_JPA_SHOW_SQL`. 10 k6 + графики + два сценария доступа.
