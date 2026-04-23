# Лабораторная работа №5: «Кинотеатр» (Cinema)

Учебный проект: **Spring Boot 4**, **Spring Data JPA**, **PostgreSQL**, **Flyway**, **Docker Compose** (приложение, БД, **pgAdmin**). Предметная область — бронирование билетов (**Film**, **Viewer**, **Ticket**): REST API, HTML-формы, аналитика по билетам. Опциональный профиль **`inmemory`** (без БД).

**Фокус лаб. 5:** подготовка данных перед нагрузочными тестами — эндпоинты **`DELETE /api/admin/clear/*`**, Python-скрипт **`tools/seed_rest_data.py`** (`requests` + **Faker**), обёртка **`tools/run-seed.sh`**. Ранее по курсу в том же репозитории: контейнеризация и миграции (**лаб. 3**), сценарии **[k6](https://k6.io/)** (**лаб. 4**).

**Фокус лаб. 6** (см. **`ТЗ_6лаба.txt`**): развёртывание на учебной ВМ, образ приложения в **Docker Hub** (**`docker compose push` / `pull`**), **лимиты CPU/RAM** сервиса `app` в Compose, **переменные окружения** для JDBC и Tomcat (`server.tomcat.threads.max`), отключение **`spring.jpa.show-sql`**, доступ через **SSH-туннель**, нагрузочные прогоны **k6 с постоянными VU** и соотношениями POST/GET **5/95, 50/50, 95/5** для графиков «время отклика vs число CPU».

**Навигация:** ниже по порядку — таблицы **эндпоинтов** (в т.ч. **OpenAPI / Swagger**), **требования**, **быстрый старт** (Docker), **обновление контейнера после правок кода**, **профили**, **миграции**, **лаб. 6** (VM, env, лимиты, k6), **лаб. 5** (сидирование), **лаб. 4** (k6), описание проекта, **структура**, **SQL**, **порты**.

## Эндпоинты

### OpenAPI / Swagger UI (документация REST)

Подключён **SpringDoc OpenAPI 3** (`springdoc-openapi-starter-webmvc-ui`). В спецификацию попадают только пути **`/api/**`** (HTML-страницы и служебные пути в Swagger не дублируются).

| Метод | Путь | Назначение |
|---|---|---|
| GET | `/v3/api-docs` | OpenAPI 3 в формате JSON. |
| GET | `/swagger-ui.html` | Интерактивная документация и **Try it out** для REST. |

На главной HTML-странице (`GET /`) в навигации есть ссылка **Swagger UI**. **Один порт** (по умолчанию **8080**) обслуживает и веб-страницы, и REST, и Swagger — это нормальная схема для Spring Boot.

**Важно:** глобальный обработчик ошибок приложения настроен только на контроллеры пакета `ru.hse.lab2.controller`, чтобы не маскировать сбои SpringDoc ответом «Unexpected server error».

### HTML (страницы и формы)

| Метод | Путь | Назначение |
|---|---|---|
| GET | `/` | Главная HTML-страница с навигацией по разделам. |
| GET | `/films/page` | Список фильмов в веб-интерфейсе. |
| GET | `/films/page/create` | Форма создания фильма. |
| POST | `/films/page/create` | Создание фильма из HTML-формы. |
| GET | `/viewers/page` | Список зрителей в веб-интерфейсе. |
| GET | `/viewers/page/create` | Форма создания зрителя. |
| POST | `/viewers/page/create` | Создание зрителя из HTML-формы. |
| GET | `/tickets/page` | Список билетов в веб-интерфейсе. |
| GET | `/tickets/page/create` | Форма создания билета. |
| POST | `/tickets/page/create` | Создание билета из HTML-формы. |

### REST API: Films

| Метод | Путь | Назначение |
|---|---|---|
| GET | `/api/films` | Получить список фильмов. |
| POST | `/api/films` | Создать новый фильм. |
| GET | `/api/films/{id}` | Получить фильм по идентификатору. |
| PUT | `/api/films/{id}` | Обновить фильм по идентификатору. |
| DELETE | `/api/films/{id}` | Удалить фильм по идентификатору. |

### REST API: Viewers

| Метод | Путь | Назначение |
|---|---|---|
| GET | `/api/viewers` | Получить список зрителей. |
| POST | `/api/viewers` | Создать нового зрителя. |
| GET | `/api/viewers/{id}` | Получить зрителя по идентификатору. |
| PUT | `/api/viewers/{id}` | Обновить зрителя по идентификатору. |
| DELETE | `/api/viewers/{id}` | Удалить зрителя по идентификатору. |

### REST API: Tickets

| Метод | Путь | Назначение |
|---|---|---|
| GET | `/api/tickets` | Получить список билетов. |
| POST | `/api/tickets` | Создать новый билет. |
| GET | `/api/tickets/{id}` | Получить билет по идентификатору. |
| PUT | `/api/tickets/{id}` | Обновить билет по идентификатору. |
| DELETE | `/api/tickets/{id}` | Удалить билет по идентификатору. |

### REST API: Analytics

| Метод | Путь | Назначение |
|---|---|---|
| GET | `/api/tickets/analytics/max-viewers?filmId=...` | Найти день с максимальным числом уникальных зрителей для выбранного фильма. |
| GET | `/api/tickets/analytics/top-film-by-day?date=YYYY-MM-DD` | Найти самый посещаемый фильм за указанную дату. |

### REST API: администрирование (очистка таблиц; только БД, профиль не `inmemory`)

| Метод | Путь | Назначение |
|---|---|---|
| DELETE | `/api/admin/clear/tickets` | Очистить таблицу `tickets` (`TRUNCATE … RESTART IDENTITY CASCADE`). |
| DELETE | `/api/admin/clear/films` | Удалить билеты и фильмы. |
| DELETE | `/api/admin/clear/viewers` | Удалить билеты и зрителей. |
| DELETE | `/api/admin/clear/all` | Очистить `tickets`, `films`, `viewers`. |

Ответы: **204 No Content**. В учебном стенде без авторизации; в реальном проекте такие операции нужно защищать.

## Требования

- Java 25
- Docker + Docker Compose
- Gradle Wrapper (`./gradlew`)
- **Лаб. 4:** [k6](https://k6.io/docs/get-started/installation/) (или Docker-образ `grafana/k6`), Python 3 + `matplotlib` для графика: `pip install "matplotlib>=3.7"`
- **Лаб. 5:** Python 3.10+; зависимости сидера — `pip install -r tools/requirements-seed.txt` **или** запуск **`./tools/run-seed.sh`** (на Linux при ограничении системного `pip`, PEP 668, скрипт создаёт **`tools/.venv`** и ставит пакеты туда)

## Быстрый старт: Docker Compose

1) Поднять **весь стек** (PostgreSQL + приложение + pgAdmin).

- **Первый раз / локальная разработка:** собрать приложение и поднять всё:
  ```bash
  docker compose up -d --build
  ```
- **Сервер + образ уже в Docker Hub:** подтянуть только приложение и поднять стек:
  ```bash
  docker compose pull app
  docker compose up -d
  ```

Подробнее про **push/pull** образа **`app`** — в разделе **«Лабораторная работа №6»**, подпункт **Docker Hub**.

2) Проверить приложение:

- в логах: `docker compose logs -f app` — должно быть `Started Lab2Application`;
- в браузере: `http://localhost:8080/` (HTML), `http://localhost:8080/swagger-ui.html` (Swagger), `http://localhost:8080/v3/api-docs` (JSON OpenAPI), например `http://localhost:8080/api/films` (REST).

3) Остановить:

```bash
docker compose down
```

Полный сброс данных БД (миграции Flyway с нуля при следующем старте):

```bash
docker compose down -v
```

**Вариант для разработки на хосте:** сервис **`app` в Docker и `./gradlew bootRun` на хосте не могут одновременно слушать один и тот же порт 8080.** Либо остановите контейнер приложения (`docker stop lab2_app`), либо поднимите локальный запуск на другом порту: `SERVER_PORT=8081 ./gradlew bootRun`. Подробнее — в **«Подробный runbook»**, п. 2.

## Обновление приложения в Docker (после правок Java / HTML в контроллерах)

Имя образа задаётся **`DOCKER_IMAGE_APP`** (см. **`.env`**). Внутри контейнера **`lab2_app`** выполняется собранный **`app.jar`**. Пока не подтянут **новый** образ и не пересоздан контейнер, в браузере будет старая версия.

**Вариант A — Docker Hub (как на учебном сервере):** на машине с исходниками после правок:

```bash
docker compose build app
docker compose push app
```

На сервере:

```bash
docker compose pull app
docker compose up -d --force-recreate app
```

**Вариант B — только локально, без Hub:**

```bash
docker compose up -d --build --force-recreate app
```

Полная пересборка без кэша: **`docker compose build --no-cache app`** (затем **`push`** или **`up`**).

**Проверка, что отдаётся новая главная** (плашка с пояснением про порт в текущей версии кода отсутствует):

```bash
curl -s http://localhost:8080/ | grep -i 'Почему сайт' || echo "OK: старого текста нет"
```

## Профили запуска

### Default (без профиля)

- Используется PostgreSQL (`spring.datasource.*` в `application.properties`, URL на `localhost` при запуске с хоста)
- Включены Flyway-миграции **`V1`** (DDL) и **`V2`** (стартовые тестовые строки)
- Hibernate работает в `ddl-auto=validate`

### `docker` (запуск приложения в контейнере)

- Включается переменной `SPRING_PROFILES_ACTIVE=docker` в сервисе `app` в `docker-compose.yml`
- В `application-docker.properties` задан JDBC URL на хост БД в сети Compose: `jdbc:postgresql://postgresdb:5432/lab2_db` (логин/пароль те же, что в основном `application.properties`)

### `inmemory` (режим совместимости)

Запуск:

```bash
./gradlew bootRun --args='--spring.profiles.active=inmemory'
```

Особенности:

- отключены автоконфигурации DataSource/JPA/Flyway;
- используются in-memory store на `HashMap`;
- сохраняется поведение доменных инвариантов (включая аналитику max viewers и каскадное удаление зависимых ticket).

## API (REST)

Базовые CRUD ресурсы:

- `/api/films`
  - `GET /api/films`
  - `GET /api/films/{id}`
  - `POST /api/films`
  - `PUT /api/films/{id}`
  - `DELETE /api/films/{id}`
- `/api/viewers`
  - `GET /api/viewers`
  - `GET /api/viewers/{id}`
  - `POST /api/viewers`
  - `PUT /api/viewers/{id}`
  - `DELETE /api/viewers/{id}`
- `/api/tickets`
  - `GET /api/tickets`
  - `GET /api/tickets/{id}`
  - `POST /api/tickets`
  - `PUT /api/tickets/{id}`
  - `DELETE /api/tickets/{id}`
  - `GET /api/tickets/analytics/max-viewers?filmId={id}`
  - `GET /api/tickets/analytics/top-film-by-day?date=YYYY-MM-DD`

## HTML страницы

- `GET /` - главная страница навигации
- `GET /films/page` - список фильмов
- `GET /films/page/create` - форма создания фильма
- `GET /viewers/page` - список зрителей
- `GET /viewers/page/create` - форма создания зрителя
- `GET /tickets/page` - список билетов
- `GET /tickets/page/create` - форма создания билета

Формы создают сущности через POST:

- `POST /films/page/create`
- `POST /viewers/page/create`
- `POST /tickets/page/create`

## Миграции и БД

Схема и небольшой стартовый набор строк задаются **Flyway** при старте приложения; массовое наполнение перед k6 — через **`tools/`** (лаб. 5).

- Каталог: `src/main/resources/db/migration`
  - `V1__create_schema.sql` — **DDL** (создание таблиц `viewers`, `films`, `tickets`, ключи и ограничения)
  - `V2__seed_test_data.sql` — **DML** (небольшой набор `INSERT` и `setval` для последовательностей)
- Для **большого** объёма данных перед k6 можно дополнительно использовать `tools/seed_rest_data.py` (см. **лаб. 5**): он вызывает `DELETE /api/admin/clear/...` и создаёт сущности через REST.
- Flyway применяет скрипты при старте приложения (в контейнере или при `./gradlew bootRun`).
- Таблицы домена: `films`, `viewers`, `tickets`.

## Postman

Артефакты:

- `postman/cinema-lab2.postman_collection.json`
- `postman/local.postman_environment.json`

Как запустить smoke:

1) Импортировать collection и environment в Postman.
2) Выбрать environment `Cinema LAB2 Local`.
3) Выполнить базовый сценарий: создать `film`, `viewer`, `ticket`, затем вызвать аналитику.

## Что перенесено из lab1 и что изменено

Перенесено:

- бизнес-сущности `Film/Viewer/Ticket`;
- CRUD сценарии в REST и HTML;
- прикладной сценарий аналитики max viewers.

Изменено/не перенесено намеренно:

- канонический формат id после merge: `Long` (JPA/DB identity);
- legacy UUID back-compat из lab1 не поддерживается;
- источником данных по умолчанию является PostgreSQL (не HashMap).

Подробнее по контрактам:

- `docs/merge-contract-lab1-lab2.md`
- `docs/domain-id-api-contract.md`

## Проверка и runbook

- Пошаговый runbook: `docs/RUNBOOK.md`
- Verification checklist (Task 07): `docs/verification-checklist.md`

## Подробный runbook

**Требования:** Java 25, Docker Desktop (или Docker Engine + Compose), Gradle Wrapper (`./gradlew`).

### 1. Полный стенд: БД + приложение + pgAdmin в Compose

В корне проекта:

```bash
docker compose up -d --build
```

После изменения кода: **локально** — `docker compose up -d --build --force-recreate app`; **через Hub** — `docker compose build app && docker compose push app`, на сервере — `docker compose pull app && docker compose up -d --force-recreate app`.

Дождитесь **Healthy** у `postgresdb` и успешного старта контейнера приложения (`docker compose ps`, `docker compose logs app`).

### 2. Запуск приложения на хосте при БД в Docker

Поднимите **только инфраструктуру**, если не хотите конфликт порта **8080** с контейнером `lab2_app`:

```bash
docker compose up -d postgresdb pgadmin
```

Либо поднимите всё, но тогда **остановите** приложение в Docker перед локальным запуском:

```bash
docker stop lab2_app
./gradlew bootRun
```

Если **`lab2_app` запущен** на `8080`, `bootRun` завершится с ошибкой *Port 8080 was already in use*. Варианты: остановить контейнер (см. выше) или запустить локально на другом порту:

```bash
SERVER_PORT=8081 ./gradlew bootRun
```

или:

```bash
./gradlew bootRun --args='--server.port=8081'
```

Альтернатива в IntelliJ IDEA: переменная окружения **`SERVER_PORT=8081`** в Run Configuration или аргумент **`--server.port=8081`**, если Docker держит **8080**.

### 2.1 Режим совместимости `inmemory` (opt-in)
По умолчанию приложение работает в режиме `JPA + Flyway + PostgreSQL`.

Для запуска режима совместимости с HashMap-данными явно включите профиль:
```bash
./gradlew bootRun --args='--spring.profiles.active=inmemory'
```

В этом профиле:
- отключаются `DataSource/JPA/Flyway` автоконфигурации;
- используются in-memory хранилища на базе `HashMap`;
- бизнес-инварианты согласованы с JPA: аналитика `/api/tickets/analytics/max-viewers` считает `DISTINCT` зрителей по дню, а удаление `Film/Viewer` каскадно удаляет связанные `Ticket`;
- поднимаются демо-данные для базовых сценариев (`films`, `viewers`, `tickets`).

### 3. Проверка запуска приложения

В логах контейнера `app` или консоли `bootRun` должно быть: `Started Lab2Application in ... seconds`. После старта Flyway применит миграции `V1` и `V2`.

Корневой URL (`http://localhost:8080/`, если порт не меняли) обслуживается `HtmlPageController` и возвращает HTML home page. Документация REST: **`/swagger-ui.html`**, **`/v3/api-docs`**.

### 4. Postman: где файлы и smoke-check
Postman-артефакты лежат в директории `postman/`:
- `postman/cinema-lab2.postman_collection.json`
- `postman/local.postman_environment.json`

Быстрый smoke (после старта приложения, без ручного редактирования payload):
1. Импортируйте коллекцию и environment в Postman.
2. Выберите environment `Cinema LAB2 Local` (в нем уже есть `baseUrl` и id-переменные для типового запуска).
3. Последовательно выполните запросы:
   - `POST /api/films`
   - `POST /api/viewers`
   - `POST /api/tickets`
   - `GET /api/tickets/analytics/max-viewers?filmId=...`
4. Убедитесь, что первые 3 запроса возвращают `201`, аналитика — `200`.

### 5. Остановка
```bash
docker compose down
```
---
## Описание проекта
Проект демонстрирует работу с PostgreSQL через Spring Data JPA в области «Кинотеатр» и **развёртывание в Docker Compose**.
Реализована система бронирования билетов, включающая связь One-to-Many между сущностями:
- Film (Фильм): один фильм может иметь много билетов.
- Viewer (Зритель): один зритель может купить много билетов.
- Ticket (Билет): связующая сущность, которая ассоциирует зрителя с конкретным фильмом, датой и местом.

### Шаблонный проект (референс)
Текущая реализация повторяет структуру и ключевые практики шаблонного проекта из Bitbucket (ветка со Spring Data JPA + PostgreSQL), но в доменной области «Кинотеатр».
Краткое соответствие «в шаблоне -> в этом проекте»:
- JPA-сущности и таблицы -> `Film`, `Viewer`, `Ticket`; таблицы `films`, `viewers`, `tickets` создаются Flyway-миграцией `V1__create_schema.sql`.
- Связи One-to-Many / Many-to-One -> `Film 1:N Ticket` и `Viewer 1:N Ticket` через `@OneToMany` и `@ManyToOne`.
- Репозитории Spring Data -> отдельные `FilmRepository`, `ViewerRepository`, `TicketRepository` (на базе `JpaRepository`).
- Кастомный JPQL-запрос -> аналитический запрос в `TicketRepository` для поиска дня с максимальным числом зрителей по фильму.
- Инициализация схемы и базовых тестовых данных -> Flyway-миграции `V1__create_schema.sql` и `V2__seed_test_data.sql` применяются при старте; при необходимости — доп. сид **лаб. 5** (`tools/seed_rest_data.py`).
- PostgreSQL-конфигурация -> подключение к PostgreSQL (Docker Compose), в контейнере приложения — профиль `docker` и `application-docker.properties`.
- Контейнеризация (лаб. 3) -> `Dockerfile` (multi-stage, Java 25), сервис `app` в `docker-compose.yml`.
Ссылка на шаблон (ветка `feature/spring-boot-data-jpa`): https://bitbucket.org/zil-courses/hl-module1/src/feature/spring-boot-data-jpa/
###  Что реализовано:
-  Контейнеризация Spring Boot (`Dockerfile`) и запуск приложения вместе с БД в `docker compose`
-  Подключение PostgreSQL через Docker
- Создание сущностей с аннотациями JPA
- Репозитории для работы с БД
- Создание схемы БД через Flyway-миграции
- Наполнение БД тестовыми данными через Flyway (`V2`) и при необходимости через Python-скрипт (лаб. 5)
- Визуальное управление через pgAdmin
  #### Техническая часть
 - Инфраструктура (Docker): PostgreSQL, pgAdmin и приложение в одном `docker-compose.yml`; образ приложения собирается из `Dockerfile`.
 -  ORM-маппинг (JPA): Hibernate работает в режиме валидации схемы (`ddl-auto=validate`), а создание структуры и базовый сид выполняет Flyway; массовое сидирование — опционально скрипт **лаб. 5**.
 -  Типизация данных: Корректное маппинг Java-типов (LocalDate, LocalTime, Double) на типы данных PostgreSQL (DATE, TIME, DOUBLE PRECISION).
 -  Аналитика (JPQL): Реализация кастомного запроса в репозитории для группировки и поиска дня с максимальной посещаемостью конкретного фильма.
  #### Бизнес-логика (Домен «Кинотеатр»)
  Сущности:
- Film (Фильм): название, жанр, длительность.
- Viewer (Зритель): имя, уникальный email.
- Ticket (Билет): место, цена, дата и время сеанса.

Связи:
-  One-to-Many: Один фильм может иметь много билетов.
-  One-to-Many: Один зритель может купить много билетов.
- Целостность данных: Настройка каскадных операций (cascade = ALL) и автоудаления сирот (orphanRemoval = true) — билет удаляется автоматически при удалении зрителя или фильма.
- Инициализация (Data Seeding): базовый набор через Flyway `V2__seed_test_data.sql`; расширенное — через `tools/seed_rest_data.py` после `DELETE /api/admin/clear/...`.
- Управление: Возможность просмотра и редактирования данных через веб-интерфейс pgAdmin.
---

## Используемые технологии

| Технология | Версия | Назначение |
|------------|--------|------------|
| Java | 25 | Язык программирования |
| Spring Boot | 4.0.3 | Фреймворк |
| Spring Data JPA | - | Работа с БД |
| Hibernate | управляется Spring Boot 4.0.3 | ORM |
| PostgreSQL | 15-alpine (образ в compose) | База данных |
| Docker / Compose | - | Контейнеры БД, pgAdmin и приложения |
| Dockerfile | multi-stage Temurin 25 | Сборка и запуск Spring Boot в контейнере |
| Gradle | wrapper | Сборка и запуск проекта |
| SpringDoc OpenAPI | 3.x (starter webmvc-ui) | `/v3/api-docs`, Swagger UI |
| Python | 3.10+ | Сидирование: `requests`, `faker`; график k6: `matplotlib` |
| k6 | см. [документацию](https://k6.io/docs/) | Нагрузочное тестирование (лаб. 4) |

---

## Структура проекта

```text
lab2_rovnyagin/
├── Dockerfile                      # Образ приложения (multi-stage)
├── src/main/java/ru/hse/lab2/
│   ├── Lab2Application.java        # Точка входа
│   ├── config/OpenApiConfig.java   # Заголовок/описание OpenAPI для Swagger
│   ├── controller/                 # REST, HTML (`HtmlPageController`), admin clear, `GlobalExceptionHandler`
│   ├── entity/
│   │   ├── Film.java               # Сущность "Фильм"
│   │   ├── Viewer.java             # Сущность "Зритель"
│   │   └── Ticket.java             # Сущность "Билет" (связка)
│   └── repository/
│       ├── FilmRepository.java     # CRUD для фильмов
│       ├── ViewerRepository.java   # CRUD для зрителей
│       └── TicketRepository.java   # CRUD + аналитические запросы
├── src/main/resources/
│   ├── application.properties       # Конфигурация (хост: localhost), порт, `springdoc.paths-to-match=/api/**`
│   ├── application-docker.properties # URL БД для контейнера (postgresdb)
│   └── db/migration/               # Flyway: V1 DDL, V2 DML
├── docker-compose.yml # postgresdb + app + pgadmin; лаб. 6: limits, env для JDBC/Tomcat/JPA
├── .env.example        # Шаблон переменных для Compose (скопировать в .env)
├── tools/              # Лаб. 5: seed_rest_data.py, run-seed.sh, requirements-seed.txt
├── k6/                 # Лаб. 4/6: cinema-mixed.js, cinema-lab6-constant.js, run-sweep.sh, run-lab6-ratio-sweep.sh
└── README.md                       # Этот файл
```

## Структура базы данных

При первом запуске Flyway применяет SQL-миграции из `src/main/resources/db/migration`:
- `V1__create_schema.sql` — создание структуры БД;
- `V2__seed_test_data.sql` — небольшой набор тестовых строк.

Дополнительно большой объём данных перед k6 можно создать скриптом `tools/seed_rest_data.py` (лаб. 5).

Таблицы создаются Flyway (SQL-скриптами), а не Hibernate. Hibernate работает в режиме валидации схемы (`spring.jpa.hibernate.ddl-auto=validate`).

### Проверка Flyway
Подключитесь к PostgreSQL и выполните:
```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```
Ожидаемо должны присутствовать успешные записи для версий `1` и `2`.

### Таблица `viewers`

| Поле | Тип данных | Ограничения | Описание |
|------|------------|-------------|----------|
| `id` | `BIGSERIAL` | `PRIMARY KEY`, `NOT NULL` | Уникальный идентификатор (автоинкремент) |
| `name` | `VARCHAR(255)` | `NOT NULL` | Имя зрителя |
| `email` | `VARCHAR(255)` | `UNIQUE`, `NOT NULL` | Адрес электронной почты |

### Таблица `films`

| Поле | Тип данных | Ограничения | Описание |
|------|------------|-------------|----------|
| `id` | `BIGSERIAL` | `PRIMARY KEY`, `NOT NULL` | Уникальный идентификатор (автоинкремент) |
| `title` | `VARCHAR(255)` | `NOT NULL` | Название фильма |
| `genre` | `VARCHAR(255)` | — | Жанр фильма |
| `duration_minutes` | `INTEGER` | — | Длительность в минутах |

### Таблица `tickets`

| Поле | Тип данных | Ограничения | Описание |
|------|------------|-------------|----------|
| `id` | `BIGSERIAL` | `PRIMARY KEY`, `NOT NULL` | Уникальный идентификатор (автоинкремент) |
| `viewer_id` | `BIGINT` | `NOT NULL`, `FOREIGN KEY` | Ссылка на `viewers.id` (владелец билета) |
| `film_id` | `BIGINT` | `NOT NULL`, `FOREIGN KEY` | Ссылка на `films.id` (фильм на сеансе) |
| `session_date` | `DATE` | `NOT NULL` | Дата сеанса |
| `session_time` | `TIME` | `NOT NULL` | Время начала сеанса |
| `seat_number` | `VARCHAR(255)` | `NOT NULL` | Номер места (напр. "A12") |
| `price` | `DOUBLE PRECISION` | — | Цена билета |
###  Схема связей
FILM (1) ───< (N) TICKET >─── (1)  VIEWER
```mermaid
erDiagram
    VIEWER ||--o{ TICKET : "покупает"
    FILM ||--o{ TICKET : "имеет сеансы"
    
    VIEWER {
        bigint id PK
        varchar name
        varchar email
    }
    FILM {
        bigint id PK
        varchar title
        varchar genre
        int duration_minutes
    }
    TICKET {
        bigint id PK
        bigint viewer_id FK
        bigint film_id FK
        date session_date
        time session_time
        varchar seat_number
        double price
    }
```
## Полезные SQL-запросы

### Просмотр данных

**Все фильмы:**
```sql
SELECT * FROM films ORDER BY title;
```
**Все зрители:**
```sql
SELECT * FROM viewers ORDER BY name;
```
**Все билеты с информацией о фильме и зрителе:**
```sql
SELECT 
    t.id AS ticket_id,
    v.name AS viewer_name,
    f.title AS film_title,
    t.session_date,
    t.session_time,
    t.seat_number,
    t.price
FROM tickets t
JOIN viewers v ON t.viewer_id = v.id
JOIN films f ON t.film_id = f.id
ORDER BY t.session_date, t.session_time;
```
### Аналитика
**Максимальное количество зрителей на фильме за день (из задания):**
Считаются уникальные зрители за день (эквивалент бизнес-логики max viewers).
```sql
SELECT 
    f.title AS film_title,
    t.session_date,
    COUNT(DISTINCT t.viewer_id) AS viewer_count
FROM tickets t
JOIN films f ON t.film_id = f.id
WHERE t.film_id = :filmId -- placeholder: подставьте filmId из API-контракта
GROUP BY f.title, t.session_date
ORDER BY viewer_count DESC, t.session_date ASC
LIMIT 1;
```
**Количество билетов по каждому фильму:**
```sql
SELECT 
    f.title AS film_title,
    COUNT(t.id) AS tickets_sold,
    SUM(t.price) AS total_revenue
FROM films f
LEFT JOIN tickets t ON f.id = t.film_id
GROUP BY f.id, f.title
ORDER BY tickets_sold DESC;
```
**Средняя цена билета по жанрам:**
```sql
SELECT 
    f.genre,
    COUNT(t.id) AS tickets_count,
    ROUND(AVG(t.price), 2) AS avg_price
FROM films f
JOIN tickets t ON f.id = t.film_id
GROUP BY f.genre
ORDER BY avg_price DESC;
```
**Зрители, купившие больше одного билета:**
```sql
SELECT 
    v.name,
    v.email,
    COUNT(t.id) AS tickets_count
FROM viewers v
JOIN tickets t ON v.id = t.viewer_id
GROUP BY v.id, v.name, v.email
HAVING COUNT(t.id) > 1
ORDER BY tickets_count DESC;
```
### Управление данными
**Добавить нового зрителя:**
```sql
INSERT INTO viewers (name, email) 
VALUES ('Анна Смирнова', 'anna@test.ru');
```
**Удалить зрителя:**
```sql
DELETE FROM viewers WHERE id = 1;
```
**Удалить фильм:**
```sql
DELETE FROM films WHERE id = 1;
```
**Забронировать билет:**
```sql
INSERT INTO tickets (viewer_id, film_id, session_date, session_time, seat_number, price)
VALUES (1, 2, '2026-04-25', '19:00', 'C5', 500.0);
```
**Удалить все билеты на определенную дату:**
```sql
DELETE FROM tickets WHERE session_date = '2026-04-20';
```
**Обновить цену билета:**
```sql
UPDATE tickets 
SET price = 600.0 
WHERE film_id = 1 AND session_date = '2026-04-25';
```

## Лабораторная работа №6: ВМ, Docker, env, лимиты ресурсов, SSH, k6

Текст задания: **`ТЗ_6лаба.txt`**. Ниже — что уже **реализовано в репозитории** и как этим пользоваться на сервере и локально.

### Подготовка на ВМ (выполняете вы)

- Подключение по SSH (порт из [таблицы курса](https://docs.google.com/spreadsheets/); пример для порта **2303**):  
  `ssh -p 2303 hl@hlssh.zil.digital`
- Обновление ОС, **`~/.ssh/authorized_keys`** со своим `id_rsa.pub`, **git**, ключ для GitHub, **`docker login`**, клонирование репозитория — по методичке; пароли пользователей **`hl`** и **root** не менять.

### Образ приложения в Docker Hub (сборка → push, на сервере → pull)

Сервис **`app`** в **`docker-compose.yml`** использует **`image: ${DOCKER_IMAGE_APP:-…}`** и **`build:`**: локально (или в CI) образ **собирается и тегируется** тем же именем, что потом пушится в Hub; **на учебной ВМ** достаточно **`git pull`** репозитория (compose-файл + `.env`), **`docker login`** и подтянуть образ **без сборки Gradle на сервере**.

1. На [Docker Hub](https://hub.docker.com/) создайте **публичный** репозиторий, например **`lab2_rovnyagin`** (имя должно совпадать с путём образа).
2. В **`.env`** задайте **`DOCKER_IMAGE_APP=<ваш_логин>/lab2_rovnyagin:latest`** (по умолчанию в репозитории указан пример **`lavrentiyermakov/lab2_rovnyagin:latest`** — замените логин при необходимости).
3. Для **`docker push`** нужен токен с правами **Read & Write** (не только *Public read-only*). Выполните **`docker login`**, затем на машине, где собираете образ:
   ```bash
   docker compose build app
   docker compose push app
   ```
4. **На сервере** (после `docker login` тем же аккаунтом, если репозиторий приватный):
   ```bash
   docker compose pull app
   docker compose up -d
   ```
   Не используйте на сервере **`docker compose up --build`**, если хотите брать только то, что уже в Hub (иначе снова пойдёт локальная сборка).

После правок Java: снова **`build` + `push`**, на сервере — **`pull app`** и **`up -d`**.

### Docker Compose: лимиты CPU/RAM и переменные окружения

В **`docker-compose.yml`** для сервиса **`app`** задано:

- **`deploy.resources.limits` / `reservations`** — верхняя граница и резерв CPU/RAM (поддерживается современным **`docker compose`**; при необходимости обновите Docker / Compose).
- Значения по умолчанию можно переопределить через **`.env`** (шаблон — **`.env.example`**) или экспорт переменных перед `docker compose up`:
  - **`APP_CPU_LIMIT`**, **`APP_MEMORY_LIMIT`**, **`APP_CPU_RESERVATION`**, **`APP_MEMORY_RESERVATION`**

**Подключение к БД и Tomcat через env** (см. [внешнюю конфигурацию Spring Boot](https://docs.spring.io/spring-boot/reference/features/external-config.html)):

| Переменная | Назначение |
|------------|------------|
| **`SPRING_DATASOURCE_URL`** | JDBC URL (в Docker по умолчанию `jdbc:postgresql://postgresdb:5432/lab2_db`) |
| **`SPRING_DATASOURCE_USERNAME`**, **`SPRING_DATASOURCE_PASSWORD`** | Учётка БД |
| **`POSTGRES_DB`**, **`POSTGRES_USER`**, **`POSTGRES_PASSWORD`** | Инициализация контейнера PostgreSQL; **имя БД в URL и в `POSTGRES_DB` должно совпадать** (для строки из таблицы, например **hl3**: и `POSTGRES_DB=hl3`, и `SPRING_DATASOURCE_URL=jdbc:postgresql://postgresdb:5432/hl3`) |
| **`SERVER_TOMCAT_THREADS_MAX`** | Эквивалент свойства **`server.tomcat.threads.max`** (в старых версиях ТЗ фигурировало `server.tomcat.max-threads`) |
| **`SPRING_JPA_SHOW_SQL`** | `true` / `false` — вывод SQL Hibernate в лог |
| **`SPRING_JPA_PROPERTIES_HIBERNATE_FORMAT_SQL`** | форматирование SQL в логе |
| **`DOCKER_IMAGE_APP`** | Полное имя образа приложения на Hub, например **`логин/lab2_rovnyagin:latest`** |

В **`application.properties`** заданы плейсхолдеры для **`spring.jpa.show-sql`**, **`hibernate.format_sql`** и **`server.tomcat.threads.max`**, чтобы локально без Docker можно было задавать те же переменные.

В Docker по умолчанию **`SPRING_JPA_SHOW_SQL=false`** (тише логи контейнера).

### SSH-туннель к приложению на сервере

Если приложение в контейнере слушает **8080** на ВМ:

```bash
ssh -p <ВАШ_SSH_ПОРТ> -L 8080:localhost:8080 hl@hlssh.zil.digital
```

Дальше на своём ПК: **`http://localhost:8080/`**, **`http://localhost:8080/swagger-ui.html`**.

### Swagger

OpenAPI/Swagger уже подключены (**`/swagger-ui.html`**, **`/v3/api-docs`**). После деплоя проверьте в браузере или `curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/v3/api-docs`.

### Нагрузочное тестирование (лаб. 6, п. 10)

Нужны **постоянные VU** и три соотношения **вставка/чтение** (POST создание фильма / GET аналитики): **5/95**, **50/50**, **95/5** — задаётся **`POST_SHARE`** (`0.05`, `0.5`, `0.95`).

- Сценарий: **`k6/cinema-lab6-constant.js`**
- Три прогона подряд: **`./k6/run-lab6-ratio-sweep.sh`** (результаты: **`k6/reports/lab6-summary-*.json`**)

Переменные: **`BASE_URL`**, **`TARGET_VUS`**, **`DURATION`**, **`FILM_ID`** (в БД должны быть билеты на этот фильм — Flyway **`V2`** или сидер лаб. 5).

**Серия «время отклика vs число CPU»:** на ВМ меняйте **`APP_CPU_LIMIT`** (шаг **0.5** vCPU) в **`.env`**, перезапустите приложение: **`docker compose up -d --force-recreate app`** (если образ с Hub не менялся) или полный цикл **`pull` + `up`** после публикации нового образа; затем снова k6. Повторите для соотношений 5/95, 50/50, 95/5.

По ТЗ прогоны нужны **с локальной машины на сервер** (часто `BASE_URL=http://localhost:8080` при открытом SSH-туннеле или прямой URL сервера) и **с сервера на сервер** (`BASE_URL=http://localhost:8080` при запуске k6 на той же ВМ).

### Быстрый чеклист после правок кода

**На машине разработчика** (сборка и публикация образа):

```bash
docker compose build app
docker compose push app
```

**На сервере** (только подтягивание образа и перезапуск):

```bash
docker compose pull app
docker compose up -d --force-recreate app
```

Локально без Hub по-прежнему можно собрать и запустить: **`docker compose build app && docker compose up -d`**.

---

## Лабораторная работа №5: сидирование REST (Python + Faker)

Цель: при необходимости перед нагрузочным прогоном **очистить** таблицы и **массово** заполнить API тестовыми данными через **HTTP** (базовый набор уже вносит Flyway **`V2`**).

### Скрипт

| Файл | Назначение |
|------|------------|
| `tools/seed_rest_data.py` | `--base-url`, `--endpoint` **`films` \| `viewers` \| `tickets` \| `all`**, `--count` (по умолчанию **500**; при **`--clear`** не нужен). **`--clear`** — только очистка: `films` → clear/films; `viewers` → clear/viewers; `tickets` → те же билеты+зрители (`clear/viewers`); `all` → clear/all. Сидирование: `films`/`viewers` — как раньше; **`tickets`** — `clear/tickets`, затем **`max(1, count // divisor)`** фильмов и столько же зрителей (`--divisor`, по умолчанию **10**), затем **`count`** билетов; **`all`** — по **`count`** каждого типа. |
| `tools/run-seed.sh` | Обёртка в духе `k6/run-sweep.sh`. Если нет `requests`/`faker`, создаёт **`tools/.venv`** и ставит зависимости туда (удобно при **PEP 668** / запрете системного `pip`). Без аргументов — `BASE_URL` / `ENDPOINT` / `COUNT` из окружения; с аргументами — проксирует в `seed_rest_data.py`. Переменная **`NO_PIP_INSTALL=1`** — не трогать venv/pip. |
| `tools/requirements-seed.txt` | `requests`, `faker` |

Установка зависимостей (вручную, если не используете `run-seed.sh`):

```bash
pip install -r tools/requirements-seed.txt
# либо виртуальное окружение:
python3 -m venv tools/.venv && tools/.venv/bin/pip install -r tools/requirements-seed.txt
```

### Типовая цепочка: стенд → сид → k6

1. Поднять API с актуальным кодом (локально: **`docker compose up -d --build --force-recreate app`**; через Hub: **`build` + `push`**, на стенде **`pull app` + `up -d`**).
2. **`./tools/run-seed.sh`** (или `COUNT=100 ENDPOINT=all ./tools/run-seed.sh`).
3. **`./k6/run-sweep.sh`** (при необходимости задать **`FILM_ID`** на существующий фильм — см. `GET /api/films`).

### Проверка и частые ситуации

| Симптом | Что делать |
|--------|------------|
| `GET /api/films` → `[]` | После **`clear`** таблицы пустые — снова запустите сидер; либо проверьте, что смотрите ту же БД, что и приложение. |
| `DELETE /api/admin/clear/...` → не **204** | Профиль **`inmemory`** не поддерживает clear; убедитесь, что образ **`app`** пересобран с классами `AdminMaintenance*`. |
| Данные в pgAdmin есть, API пустой | Разные хост/БД/порт в настройках подключения; сравните с `application.properties` / `application-docker.properties`. |
| `/swagger-ui.html` или `/v3/api-docs` → **500**, JSON с `"Unexpected server error"` | Часто **старый** образ или сбой SpringDoc; **`docker logs lab2_app`**. Пересоберите/подтяните образ и пересоздайте контейнер (**`pull app` + `up -d --force-recreate app`** или **`up -d --build --force-recreate app`** локально). |
| Правили Java, в браузере всё по-старому | С Hub: **`push`** нового образа, на сервере **`pull app`** и **`up -d --force-recreate app`**. Локально: **`up -d --build --force-recreate app`**. |
| `bootRun`: *Port 8080 was already in use* | На **8080** уже слушает **`lab2_app`**: `docker stop lab2_app` или локальный запуск с **`SERVER_PORT=8081`**. |

**IntelliJ IDEA:** Run **`Lab2Application`** (профиль не **`inmemory`**, PostgreSQL доступна), затем в терминале IDE: **`./tools/run-seed.sh`** или Run-конфигурация для **`seed_rest_data.py`** с аргументами `--endpoint all --count 50`.

Примеры:

```bash
# то же через shell (по умолчанию all, count 500, BASE_URL из окружения)
./tools/run-seed.sh

ENDPOINT=films COUNT=100 ./tools/run-seed.sh

# только фильмы (связанные билеты очищаются на стороне сервера)
python3 tools/seed_rest_data.py --endpoint films --count 500

# фильмы + зрители + билеты (по count записей каждого типа)
python3 tools/seed_rest_data.py --endpoint all --count 200

python3 tools/seed_rest_data.py --endpoint tickets --count 100 --divisor 10
python3 tools/seed_rest_data.py --endpoint viewers --clear
python3 tools/seed_rest_data.py --endpoint tickets --clear

./tools/run-seed.sh --endpoint all --count 50 --base-url http://localhost:8080
```

Для `--endpoint tickets` в БД уже должны быть фильмы и зрители (или сначала выполните `--endpoint all` / отдельно `films` и `viewers`).

После `--endpoint all` или `--endpoint films` первый созданный фильм обычно имеет **`id = 1`** (после полной очистки), что удобно для **`FILM_ID=1`** в k6. Без скрипта после чистого Flyway **`V2`** тоже создаёт фильм с **`id = 1`**.

**Профиль `inmemory`:** эндпоинты `/api/admin/clear/*` **не** поднимаются; сидер рассчитан на работу с PostgreSQL.

---

## Лабораторная работа №4: нагрузочное тестирование (k6)

Цель: смоделировать нагрузку на API с **растущим числом виртуальных пользователей (VU)** и построить **график среднего времени отклика** (`http_req_duration.avg`, мс) **от целевых VU**.

### Что тестируется

| Направление | Метод | Эндпоинт | Назначение |
|-------------|--------|----------|------------|
| Создание «простой» сущности (без ссылок на другие сущности в теле запроса) | `POST` | `/api/films` | JSON: `title`, `genre`, `durationMinutes` |
| Статистика (агрегация по билетам/фильму) | `GET` | `/api/tickets/analytics/max-viewers?filmId=…` | Аналитика; для сида обычно `filmId=1` |

Доля нагрузки **POST / GET** задаётся **`POST_SHARE`** в `[0..1]`: **`TARGET_VUS`** делится между двумя параллельными сценариями k6 (пул только POST и пул только GET), без случайного выбора внутри одной итерации.

### Скрипты (коммитятся в репозиторий)

| Файл | Описание |
|------|----------|
| `k6/cinema-mixed.js` | Два параллельных сценария **`ramping-vus`**: **`post_films`** (POST `/api/films`) и **`get_analytics`** (GET аналитики); доля VU между ними — **`POST_SHARE`** от **`TARGET_VUS`**, пакет **`k6/http`**. |
| `k6/run-sweep.sh` | Серия прогонов **10 → 20 → 40 → 80 → 160**; перед стартом **очищает** `k6/reports` (JSON/PNG); после прогонов при необходимости ставит **`matplotlib`** и строит **`avg_vs_vus.png`**. Опции: **`NO_CLEAN=1`**, **`NO_PLOT=1`**, **`USE_DOCKER_K6=1`**. |
| `k6/plot_avg_vs_vus.py` | Читает `summary-vus-*.json`, строит **`k6/reports/avg_vs_vus.png`**: две линии **POST** и **GET** по метрикам `k6_post_film_ms` / `k6_get_analytics_ms` из `cinema-mixed.js`; для старых JSON без них — одна линия по `http_req_duration`. |

Сгенерированные **`*.json` / `*.png`** в `k6/reports/` по умолчанию в **`.gitignore`** (в коммит кладутся сами сценарии и генератор графика).

### Подготовка

1. Запустите API (например `docker compose up -d` или `./gradlew bootRun`), убедитесь, что доступен **`http://localhost:8080`**. После применения Flyway **`V2`** для аналитики обычно подходит **`FILM_ID=1`**. Для большой выборки перед k6 можно выполнить **`python3 tools/seed_rest_data.py --endpoint all --count …`**. Иначе задайте **`FILM_ID`** на существующий фильм.

2. Установите k6 **или** используйте Docker (см. ниже).

3. **matplotlib** для графика подтянет сам **`./k6/run-sweep.sh`** (`pip install --user` при первом запуске). Вручную при необходимости: `pip install "matplotlib>=3.7"`.

### Один прогон (ручной пример)

```bash
export BASE_URL=http://localhost:8080
export TARGET_VUS=20
export POST_SHARE=0.5
export FILM_ID=1
k6 run k6/cinema-mixed.js
```

Экспорт метрик в JSON:

```bash
k6 run --summary-export k6/reports/summary-vus-20.json k6/cinema-mixed.js
```

### Серия прогонов и график (рекомендуется для отчёта)

Одна команда: очистка старых отчётов в `k6/reports`, пять прогонов k6, затем график **`avg_vs_vus.png`** (и при необходимости установка matplotlib в user-site):

```bash
./k6/run-sweep.sh
```

Не удалять прошлые JSON/PNG перед прогоном: **`NO_CLEAN=1 ./k6/run-sweep.sh`**. Только метрики без графика: **`NO_PLOT=1 ./k6/run-sweep.sh`**.

Переменные окружения для sweep: **`BASE_URL`**, **`FILM_ID`**, **`POST_SHARE`** (как в `cinema-mixed.js`).

Если **k6 не установлен**, но есть Docker:

```bash
USE_DOCKER_K6=1 BASE_URL=http://host.docker.internal:8080 ./k6/run-sweep.sh
```

На Linux при необходимости скрипт добавляет `host.docker.internal` через `host-gateway`. Если обращение к API не проходит, используйте IP хоста или опубликуйте порт приложения в сети Docker.

### Критерии соответствия ТЗ (кратко)

- Используются **`ramping-vus`** и **`k6/http`**.
- Нагрузка: **POST** создание простой сущности + **GET** статистики; **два параллельных пула** VU, пропорция **`POST_SHARE`** от **`TARGET_VUS`**.
- Несколько уровней нагрузки (**4–5 точек**, удвоение VU) и **график avg vs VU** через `plot_avg_vs_vus.py`.

## Локальные адреса и порты

| Адрес | Сервис | Назначение |
|-------|--------|------------|
| `http://localhost:8080` | Spring Boot | Одно приложение: HTML (`/`, `/films/page`, …), REST (`/api/...`), Swagger (`/swagger-ui.html`), OpenAPI JSON (`/v3/api-docs`). Типично контейнер **`lab2_app`** или локальный `bootRun`. |
| `http://localhost:8080/swagger-ui.html` | Swagger UI | Документация и вызовы REST (тот же порт, что и приложение). |
| `http://localhost:8080/v3/api-docs` | OpenAPI | Машиночитаемая спецификация API. |
| `http://localhost:15432` | pgAdmin 4 | Веб-интерфейс для визуального управления БД |
| `localhost:5432` | PostgreSQL | База данных (используется приложением для подключения) |

Порт приложения можно переопределить: **`SERVER_PORT=8081`** или **`--server.port=8081`** (тогда все URL выше — с **8081**).

### Учетные данные

**pgAdmin (доступ через браузер):**
- **Email:** `admin@admin.com`
- **Password:** `admin_password`

**PostgreSQL (для приложения Spring Boot):**
- **Username:** `postgres`
- **Password:** `lab2_password`
- **Database:** `lab2_db`
- **Host:** `localhost`
- **Port:** `5432`

**PostgreSQL (для добавления сервера в pgAdmin):**
- **Host name/address:** `postgresdb` (имя сервиса в `docker-compose.yml`)
- **Port:** `5432`
- **Maintenance DB:** `lab2_db`
- **Username:** `postgres`
- **Password:** `lab2_password`
