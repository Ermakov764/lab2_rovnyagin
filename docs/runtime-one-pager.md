# Runtime one-pager

Полная версия: **`README.md`**, задание: **`ТЗ_6лаба.txt`**.

---

## Суть

ВМ с приложением -> **Docker Compose** + конфиг из **`.env`** -> доступ с ПК по **SSH `-L 8080:localhost:8080`** -> **k6** (const VU, смеси 5/95, 50/50, 95/5) -> графики по CPU.

---

## Порты (не путать)

| Порт | Где | Что |
|------|-----|-----|
| **8080** | ВМ / туннель с ПК | HTTP приложения (HTML, REST, Swagger) |
| **5433** | hl12 | PostgreSQL (внешний порт БД для JDBC/psql) |
| **2303** *(пример)* | ПК -> ВМ | SSH персональной ВМ (см. таблицу курса) |
| **2311** *(пример)* | hl03 -> k6-ВМ | SSH общей машины с k6 |

---

## ВМ: подъём с нуля

```bash
cd ~/lab2_rovnyagin
cp -n .env.example .env
# .env: DBHOST/DBPORT/DBNAME/SCHEMANAME под hl12 и корректные DOCKER_IMAGE_*
docker compose -f docker-compose.hl12.yml --env-file .env pull
docker compose -f docker-compose.hl12.yml --env-file .env up -d --force-recreate
docker compose -f docker-compose.hl12.yml --env-file .env ps
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/api/films
```

Логи при падении: `docker compose -f docker-compose.hl12.yml --env-file .env logs --tail 80 crud-app additional-app`

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
| `DBHOST` / `DBPORT` / `DBNAME` / `SCHEMANAME` | Подключение к БД на hl12 |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | Учётка БД |
| `SERVER_TOMCAT_THREADS_MAX` | Потоки Tomcat (= `server.tomcat.threads.max`) |
| `SPRING_JPA_SHOW_SQL` | `false` — тише лог (п. 9 ТЗ) |
| `APP_CPU_LIMIT` | Лимит CPU контейнера `crud-app` (шаг 0.5 для серии) |

После смены CPU: `docker compose -f docker-compose.hl12.yml --env-file .env up -d --force-recreate crud-app`

---

## k6 (п. 10)

Сценарий: **`k6/cinema-lab6-constant.js`**. Три смеси подряд: **`./k6/run-lab6-ratio-sweep.sh`**.

```bash
export TARGET_VUS=30
export DURATION=90s
export BASE_URL=http://127.0.0.1:8080   # с ПК при открытом туннеле
export K6_ROUTE=pc-to-server            # или server-to-server — попадёт в JSON и подпись PNG
# или BASE_URL=http://<IP_приложения>:8080 — с машины, откуда виден API
./k6/run-lab6-ratio-sweep.sh
```

`K6_ROUTE`: **`pc-to-server`** — k6 с ПК через SSH-туннель к `BASE_URL`; **`server-to-server`** — k6 на учебной ВМ, `BASE_URL` до приложения по сети (как в **`remote-k6-sync-and-run.sh`**, там по умолчанию уже `server-to-server`). В каждом JSON есть **`lab6_meta`** (VU, длительность, URL, маршрут) — **`plot_k6_cpu_results.py`** выводит это в заголовок графика.

Копирование отчётов по CPU: `RESULT_CPU=0.5|1.0|1.5|2 ./k6/run-lab6-ratio-sweep.sh`  
Графики: `python3 k6/plot_k6_cpu_results.py results -o png_k6` (нужен matplotlib). Скрипт требует **`lab6_meta`** во всех JSON и однородную серию прогонов — иначе выход с ошибкой.

**Тяжелее аналитика (опционально):** больше строк в БД — **`./tools/run-seed.sh`** или `COUNT=… ./tools/run-seed.sh` перед серией прогонов (один и тот же объём данных на всю серию).

**ТЗ:** два прогона — **ПК -> сервер** (туннель + k6 на ПК) и **сервер -> сервер** (k6 там, где корректен `BASE_URL`).

---

## Частые ошибки

| Симптом | Действие |
|---------|----------|
| `database "hl3" does not exist` | Согласовать `.env`; при необходимости `docker compose down -v` и `up -d` |
| `address already in use :8080` | На ПК: туннель **или** локальный Docker — не оба на один порт |
| k6 падает на GET | Сиды / `FILM_ID`: `./tools/run-seed.sh`, проверить `GET /api/films` |

---

## Полезные файлы

| Файл | Роль |
|------|------|
| `scripts/ssh-tunnel-personal-vm.sh` | Туннель 8080 с ПК на ВМ |
| `k6/cinema-lab6-constant.js` | постоянные VU; в итерации POST с вероятностью POST_SHARE |
| `k6/run-lab6-ratio-sweep.sh` | 5/95, 50/50, 95/5 подряд |
| `k6/plot_k6_cpu_results.py` | PNG из `results/cpu-*` |
| `k6/remote-k6-sync-and-run.sh` | rsync k6 + запуск на удалённой k6-ВМ |

