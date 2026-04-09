#  Лабораторная работа №2: Spring Data JPA + PostgreSQL

## Быстрый запуск
**Требования:** Java 17+, IntelliJ IDEA, Docker Desktop.

### 1. Запуск базы данных
В терминале в корне проекта выполните:
```bash
docker compose up -d
```
Дождитесь статуса Up для контейнеров postgres и pgadmin.
### 2. Запуск приложения
Откройте проект в IntelliJ IDEA, дождитесь загрузки зависимостей Maven и запустите класс Lab2Application.java
### 3. Проверка
В консоли должно появиться:
Started Lab2Application in ... seconds
✅ Тестовые данные успешно добавлены в БД!
### 4. Остановка
```bash
docker compose down
```
---
## Описание проекта
Проект демонстрирует работу с реляционной базой данных PostgreSQL через Spring Data JPA.
Реализована связь **One-to-Many** между сущностями `User` (пользователь) и `Phone` (телефон).

###  Что реализовано:
-  Подключение PostgreSQL через Docker
- Создание сущностей с аннотациями JPA
- Репозитории для работы с БД
- Автоматическое создание таблиц при запуске
- Наполнение БД тестовыми данными
- Визуальное управление через pgAdmin

---
## Описание задачи
Разработка Spring Boot приложения для работы с реляционной базой данных PostgreSQL.
Реализована связь между таблицами One-to-Many (Один ко Многим): один пользователь может иметь несколько номеров телефонов.
🛠 Использованные технологии:
Java (JDK 17/25)
Spring Boot 3 (Web, Data JPA)
Hibernate (ORM фреймворк)
PostgreSQL (СУБД)
Docker Compose (Оркестрация контейнеров)
Maven (Сборка проекта)

## Используемые технологии

| Технология | Версия | Назначение |
|------------|--------|------------|
| Java | 17/25 | Язык программирования |
| Spring Boot | 3.2.4 | Фреймворк |
| Spring Data JPA | - | Работа с БД |
| Hibernate | 6.4.4 | ORM |
| PostgreSQL | latest | База данных |
| Docker Compose | - | Контейнеризация |
| Maven | - | Сборка проекта |

---

## Структура проекта

```text
lab2_rovnyagin/
├── src/main/java/ru/hse/lab2/
│   ├── Lab2Application.java        # Точка входа приложения (Spring Boot)
│   ├── DataInitializer.java        # Класс для заполнения БД тестовыми данными
│   ├── entity/
│   │   ├── User.java               # Сущность "Пользователь" (One)
│   │   └── Phone.java              # Сущность "Телефон" (Many)
│   └── repository/
│       ├── UserRepository.java     # Интерфейс репозитория для User
│       └── PhoneRepository.java    # Интерфейс репозитория для Phone
├── src/main/resources/
│   └── application.properties      # Конфигурация подключения к БД и JPA
├── docker-compose.yml              # Настройка контейнеров (Postgres + pgAdmin)
├── pom.xml                         # Зависимости Maven (Spring, JPA, Postgres)
└── README.md                       # Описание проекта
```

## Структура базы данных

При первом запуске приложения Hibernate автоматически генерирует DDL-скрипты и создаёт таблицы на основе JPA-аннотаций в классах `User` и `Phone`.

### Таблица `users`
| Поле | Тип данных | Ограничения | Описание |
|------|------------|-------------|----------|
| `id` | `BIGSERIAL` | `PRIMARY KEY`, `NOT NULL` | Уникальный идентификатор (автоинкремент) |
| `first_name` | `VARCHAR(255)` | — | Имя пользователя |
| `last_name` | `VARCHAR(255)` | — | Фамилия пользователя |
| `email` | `VARCHAR(255)` | — | Адрес электронной почты |

### Таблица `phones`
| Поле | Тип данных | Ограничения | Описание |
|------|------------|-------------|----------|
| `id` | `BIGSERIAL` | `PRIMARY KEY`, `NOT NULL` | Уникальный идентификатор (автоинкремент) |
| `phone_number` | `VARCHAR(255)` | `NOT NULL` | Номер телефона |
| `user_id` | `BIGINT` | `NOT NULL`, `FOREIGN KEY` | Ссылка на `users.id` (владелец телефона) |

###  Схема связей
```sql
users (1) ───< (N) phones
```
## Локальные адреса и порты

| Адрес | Сервис | Назначение |
|-------|--------|------------|
| `http://localhost:8080` | Spring Boot | Основное приложение (веб-сервер / REST API) |
| `http://localhost:15432` | pgAdmin 4 | Веб-интерфейс для визуального управления БД |
| `localhost:5432` | PostgreSQL | База данных (используется приложением для подключения) |

### Учетные данные

**pgAdmin (доступ через браузер):**
- **Email:** `admin@admin.com`
- **Password:** `admin_password`

**PostgreSQL (для подключения из Spring Boot):**
- **Username:** `postgres`
- **Password:** `lab2_password`
- **Database:** `lab2_db`
- **Host (в приложении):** `lab2_postgres` (имя Docker-контейнера)