# Лабораторные работы №9-10

Проект состоит из двух Spring Boot сервисов:

- `crud-app` на порту `8080` - основной CRUD-сервис с фильмами, зрителями и билетами.
- `additional-app` на порту `8081` - дополнительный сервис аналитики.

Текущий README описывает только актуальные лабораторные:

- **Лаб. 9** - наблюдаемость, сбор статистики и k6-прогоны при CPU `0.5` и `1.0`.
- **Лаб. 10** - кеширование справочника фильмов в `additional-service`.

## Архитектура

Основной пользовательский сценарий для нагрузки:

```text
k6
  -> GET /api/cinema/films/max-viewers-summary на crud-app:8080
      -> crud-app проксирует запрос в additional-app:8081
          -> additional-app строит статистику по данным CRUD
```

`additional-service` строит отчет:

```text
GET /api/analytics/films/max-viewers-summary
```

Для расчета он использует:

- `GET /api/films` - справочник фильмов;
- `GET /api/tickets` - билеты, по которым считается статистика посещаемости.

## Важные файлы

| Файл | Назначение |
|------|------------|
| `docker-compose.hl12.yml` | запуск `crud-app` и `additional-app` с БД на hl12 |
| `.env` | адрес БД, Docker-образы, порты, CPU/RAM лимиты |
| `scripts/lab9-hl3.sh` | helper на hl03: build, restart, смена CPU, сбор docker logs |
| `scripts/lab9-hl11-run-all.sh` | полный k6-прогон на hl11: CPU `0.5` и `1.0`, смеси `5/95`, `50/50`, `95/5` |
| `k6/run-ratio-sweep.sh` | три k6-прогона для одного CPU |
| `k6/plot-from-results.sh` | построение PNG-графиков из `results/cpu-*` |
| `k6/collect-observability-after-k6.sh` | сохранение `/api/observability` после k6-прогона |
| `additional-service/src/main/java/ru/hse/lab8/additional/service/FilmCacheService.java` | кеш фильмов для лаб. 10 |
| `additional-service/src/main/java/ru/hse/lab8/additional/service/AnalyticsService.java` | расчет общей статистики |

## Лабораторная 9

### Что реализовано

В обоих сервисах есть observability-слой:

- `src/main/java/ru/hse/lab2/observability/*` - основной CRUD-сервис;
- `additional-service/src/main/java/ru/hse/lab8/additional/observability/*` - additional-service.

Он собирает статистику операций по временным окнам:

- количество запросов;
- количество ошибок;
- среднее время;
- `min`, `max`;
- `p50`, `p95`, `p99`;
- `rps`.

Статистика доступна через:

```text
GET /api/observability
GET /api/observability?window=10s
GET /api/observability/windows
```

Окна и период обновления задаются в `application.properties`:

```properties
observability.windows=${OBSERVABILITY_WINDOWS:10s,30s,1m}
observability.tick-ms=${OBSERVABILITY_TICK_MS:1000}
observability.log-on-refresh=${OBSERVABILITY_LOG_ON_REFRESH:true}
observability.log-empty-snapshots=${OBSERVABILITY_LOG_EMPTY_SNAPSHOTS:false}
```

### Запуск стенда на hl03

Из корня проекта на hl03:

```bash
cd ~/lab2_rovnyagin
bash scripts/lab9-hl3.sh prepare
```

Скрипт:

1. собирает Docker-образ основного сервиса;
2. собирает Docker-образ `additional-service`;
3. запускает compose;
4. проверяет доступность сервисов.

Проверка вручную:

```bash
curl -s http://127.0.0.1:8080/api/cinema/ping
curl -s http://127.0.0.1:8080/api/observability >/dev/null && echo "main observability ok"
curl -s http://127.0.0.1:8081/api/observability >/dev/null && echo "additional observability ok"
```

### CPU 0.5 и 1.0

CPU меняется на hl03 так:

```bash
cd ~/lab2_rovnyagin
bash scripts/lab9-hl3.sh set-cpu 0.5
bash scripts/lab9-hl3.sh set-cpu 1.0
```

Команда меняет в `.env`:

```text
APP_CPU_LIMIT
ADDITIONAL_CPU_LIMIT
```

и пересоздает контейнеры через:

```bash
docker compose -f docker-compose.hl12.yml --env-file .env up -d --force-recreate
```

### Полный автоматический k6-прогон на hl11

На k6-ВМ `hl11`:

```bash
export K6_ROOT="$HOME/ermakov_k6"
export MAIN_BASE_URL="http://10.60.3.33:8080"
export ADDITIONAL_BASE_URL="http://10.60.3.33:8081"
export HL03_SSH_HOST="hl@10.60.3.33"
export HL03_REPO_DIR="~/lab2_rovnyagin"

bash "$K6_ROOT/lab9-hl11-run-all.sh"
```

По умолчанию используются:

```text
TARGET_VUS=30
DURATION=90s
K6_ROUTE=server-to-server
```

Скрипт снимает 6 прогонов:

| CPU | POST/GET смесь |
|-----|----------------|
| `0.5` | `5/95` |
| `0.5` | `50/50` |
| `0.5` | `95/5` |
| `1.0` | `5/95` |
| `1.0` | `50/50` |
| `1.0` | `95/5` |

После каждого прогона сохраняются:

- k6 summary JSON;
- observability JSON/TXT по `crud-app`;
- observability JSON/TXT по `additional-app`;
- docker logs обоих контейнеров.

### Результаты k6

После полного прогона должны появиться:

```text
~/ermakov_k6/results/cpu-0.5/
~/ermakov_k6/results/cpu-1.0/
~/ermakov_k6/logs/
```

Проверка:

```bash
ls ~/ermakov_k6/results/cpu-0.5
ls ~/ermakov_k6/results/cpu-1.0
ls ~/ermakov_k6/logs
```

В каждой CPU-папке должны быть summary-файлы:

```text
summary-post05-get95-vus-30-cpu-0.5.json
summary-post50-get50-vus-30-cpu-0.5.json
summary-post95-get05-vus-30-cpu-0.5.json
```

и аналогичные для `cpu-1.0`.

### Построение графиков

На hl11:

```bash
cd ~/ermakov_k6
TARGET_VUS=30 bash k6/plot-from-results.sh
```

PNG появятся в:

```text
~/ermakov_k6/k6/png_k6/
```

Ожидаемые файлы:

```text
vs-cpu-mix-5-95.png
vs-cpu-mix-50-50.png
vs-cpu-mix-95-5.png
```

## Лабораторная 10

### Задача

Нужно добавить класс, который кеширует записи, необходимые для вывода общей статистики в `additional-service`, и периодически печатает статистику наполнения кеша.

В этом проекте кешируется **справочник фильмов**:

```text
filmId -> CrudFilm(id, title)
```

Почему именно фильмы:

- `additional-service` использует фильмы для вывода `filmTitle` в итоговой статистике;
- справочник фильмов меняется реже, чем билеты;
- повторные запросы аналитики часто используют один и тот же список фильмов;
- билеты не кешируются, чтобы расчет посещаемости оставался актуальным.

### Где реализовано

| Файл | Что делает |
|------|------------|
| `FilmCacheService.java` | хранит `HashMap<Long, CrudFilm>`, проверяет TTL, обновляется из CRUD |
| `AnalyticsService.java` | берет фильмы через `filmCacheService.getFilms()` |
| `application.properties` | содержит TTL и период логирования кеша |

Главное изменение в `AnalyticsService`:

```java
List<CrudFilm> films = filmCacheService.getFilms();
```

Теперь `AnalyticsService` не ходит за фильмами напрямую в CRUD. Он обращается к сервису кеша, а `FilmCacheService` сам решает:

- если кеш свежий - вернуть фильмы из памяти;
- если кеш пустой или устарел - вызвать `crudClient.fetchFilms()` и обновить `HashMap`.

### Логика TTL-кеша

У кеша есть три важных поля:

```java
private final Map<Long, CrudFilm> filmsById = new HashMap<>();
private final long ttlMs;
private long lastRefreshAtMs;
```

Логика:

```text
getFilms()
  |
  |-- кеш свежий?
  |      |
  |      |-- да -> hits++ -> вернуть List.copyOf(filmsById.values())
  |
  |-- нет -> misses++ -> crudClient.fetchFilms() -> обновить HashMap -> refreshes++ -> вернуть снимок
```

Первый запрос после старта всегда приводит к refresh, потому что `lastRefreshAtMs == 0`.

Если TTL равен 30 секундам:

- запрос через 5 секунд берет фильмы из кеша;
- запрос через 30 секунд или позже обновляет кеш из CRUD.

### Настройки кеша

В `additional-service/src/main/resources/application.properties`:

```properties
film-cache.ttl-ms=${FILM_CACHE_TTL_MS:30000}
film-cache.stats-rate-ms=${FILM_CACHE_STATS_RATE_MS:10000}
```

Значения по умолчанию:

- `FILM_CACHE_TTL_MS=30000` - кеш фильмов живет 30 секунд;
- `FILM_CACHE_STATS_RATE_MS=10000` - статистика кеша печатается каждые 10 секунд.

### Логи кеша

`FilmCacheService` печатает строку:

```text
film-cache stats: size=300 ttlMs=30000 lastRefreshAtMs=... hits=295010 misses=54 refreshes=54
```

Расшифровка:

- `size` - сколько фильмов лежит в кеше;
- `ttlMs` - TTL кеша;
- `hits` - сколько раз фильмы были взяты из кеша;
- `misses` - сколько раз кеш был пустой или устаревший;
- `refreshes` - сколько раз кеш реально обновлялся из CRUD.

Проверка логов после k6:

```bash
grep "film-cache stats" ~/ermakov_k6/logs/additional-*.log | tail -30
```

Пример результата из прогона:

```text
film-cache stats: size=300 ttlMs=30000 ... hits=295010 misses=54 refreshes=54
```

Это показывает, что большая часть обращений к справочнику фильмов обслуживается из памяти, а не через повторный `GET /api/films`.

### Docker после изменений в коде

После изменения Java-кода нужно пересобрать образ и пересоздать контейнер:

```bash
cd ~/lab2_rovnyagin
docker build -f Dockerfile.additional-service -t lavrentiyermakov/lab2_additional:lab8-20260503 .
docker compose -f docker-compose.hl12.yml --env-file .env up -d --force-recreate additional-app
```

Или полный вариант:

```bash
cd ~/lab2_rovnyagin
bash scripts/lab9-hl3.sh prepare
```

Проверка:

```bash
docker logs lab8_additional_app --tail 80 | grep "film-cache stats"
```

## Что приложить в отчет

Для лабораторной 9:

- 3 PNG-графика `vs-cpu-mix-*.png`;
- summary JSON из `results/cpu-0.5` и `results/cpu-1.0`;
- observability TXT/JSON по обоим сервисам.

Для лабораторной 10:

- описание `FilmCacheService`;
- настройки TTL;
- фрагмент логов `film-cache stats`;
- те же графики CPU `0.5` и `1.0`, снятые после включения кеша.

## Быстрый чеклист

На hl03:

```bash
cd ~/lab2_rovnyagin
bash scripts/lab9-hl3.sh prepare
```

На hl11:

```bash
export K6_ROOT="$HOME/ermakov_k6"
export MAIN_BASE_URL="http://10.60.3.33:8080"
export ADDITIONAL_BASE_URL="http://10.60.3.33:8081"
export HL03_SSH_HOST="hl@10.60.3.33"
export HL03_REPO_DIR="~/lab2_rovnyagin"
bash "$K6_ROOT/lab9-hl11-run-all.sh"
```

Построить графики:

```bash
cd ~/ermakov_k6
TARGET_VUS=30 bash k6/plot-from-results.sh
```

Проверить кеш:

```bash
grep "film-cache stats" ~/ermakov_k6/logs/additional-*.log | tail -30
```
