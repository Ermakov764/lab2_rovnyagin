# Лабораторные работы №11-12

Проект — два Spring Boot сервиса:

- **`crud-app`** (порт `8080`) — основной CRUD; в **лаб. 12** подписан на Kafka и выполняет JSON-команды из топика варианта.
- **`additional-app`** (порт `8081`) — аналитика (для полного стенда в compose).

Этот README описывает только **лаб. 11** и **лаб. 12**:

- **Лаб. 11** — инфраструктура Kafka курса: узлы **hl14** / **hl15**, Docker Swarm, **топик варианта**, таблица ресурсов, **Kafka UI** и при необходимости **доступ с ПК через SSH-туннели** (см. методичку, § про туннелирование).
- **Лаб. 12** — **интеграция приложения** с Kafka: consumer в `crud-app`, `docker-compose`, скрипт отправки сообщений, проверка по логам и REST.

## Архитектура

Стенд приложения (после `docker compose`):

```text
crud-app:8080  <->  БД
additional-app:8081  ->  запросы к crud-app для аналитики
```

Цепочка **лаб. 12** (команда из Kafka попадает в БД):

```text
scripts/send_kafka_message.py (или другой producer)
  -> Kafka: hl14.zil:9094, hl15.zil:9094 — топик варианта
      -> crud-app: @KafkaListener
          -> обработчики FILM / VIEWER / TICKET -> сервисы -> БД
```

## Важные файлы

| Файл | Назначение |
|------|------------|
| `docker-compose.hl12.yml` | `crud-app` + `additional-app`; `extra_hosts` для **`hl14.zil` / `hl15.zil`**; переменные **`KAFKA_*`** |
| `.env` | БД, образы, лимиты CPU/RAM; при необходимости `HL14_ZIL_IP`, `HL15_ZIL_IP`, `KAFKA_TOPIC` |
| `scripts/send_kafka_message.py` | отправка JSON-команды в топик (Python, `kafka-python`) |
| `src/main/java/ru/hse/lab2/kafka/*` | consumer, разбор команд, обработчики (лаб. 12) |
| `src/main/resources/application.properties` | `spring.kafka.*`, `lab.kafka.topic` |
| `scripts/lab9-hl3.sh` | вспомогательно: **сборка и подъём стенда** на персональной ВМ (`prepare`) |

Методички курса (лаб. 11): разбор Swarm и команд — в [заметке про Kafka + Swarm](https://zil.digital/blog/kafka-kraft-swarm-debian12) и в `kafka-2026.md` репозитория [hl-module2](https://bitbucket.org/zil-courses/hl-module2/src/main/kafka/kafka-2026.md).

## Лабораторная 11 — Kafka на стенде курса

### Цель

Познакомиться с **кластером Kafka** курса (брокеры в **Docker Swarm**), создать **топик по номеру варианта**, занести его в [таблицу ресурсов](https://docs.google.com/spreadsheets/d/1CoubOXgx3PPpACLwhk_1lJ7QfoCFjmLf9jEzhSnu0qM/edit), открыть **Kafka UI** с ПК и при необходимости отправлять сообщения **с локальной машины** через туннель (раздел про SSH в `kafka-2026.md`).

### Что такое hl14 и hl15

| Имя | Роль |
|-----|------|
| **hl14** | Узел с брокером Kafka (и связанным стеком) в инфраструктуре курса. В DNS стенда: **`hl14.zil`**. |
| **hl15** | Второй узел с брокером; **`hl15.zil`**. |

Это **не** персональная ВМ под твой проект (условно hl03), а **общие машины кластера**. Клиенты указывают **оба** bootstrap-адреса (`hl14.zil:…`, `hl15.zil:…`), чтобы корректно получать метаданные и работать при отказе одного брокера.

Числа **2314** / **2315** в таблице ресурсов — это **SSH-порты на `hlssh.zil.digital`** для входа на соответствующие хосты (как в задании: «залогиниться на hl14 / hl15»). **Свой** персональный порт (например **2303**) используется для **своей** ВМ, где крутится docker-compose с приложением.

### Папка варианта на персональной ВМ

На своей ВМ (после `ssh` с **твоим** портом из таблицы):

```bash
mkdir -p /home/hl/<вариант>
cd /home/hl/<вариант>
```

Подставь каталог как в задании (например `hl3` или `hl03` — как в столбце варианта/БД).

### Топик

Имя топика = **номер варианта из таблицы** (должно совпадать с тем, что потом читает приложение: `KAFKA_TOPIC` / `lab.kafka.topic`). После создания топика **заполни столбец Kafka Topic** в Google-таблице.

### Docker Swarm, сервисы и логи

На **hl14** / **hl15** (см. методичку и SSH-порты **2314** / **2315**): команды вида `docker service ls`, `docker service logs …`, просмотр stack — по инструкциям из ссылок выше.

### Kafka UI с локального ПК

Подними **отдельный терминал на ПК** с пробросом порта UI. Если на ПК **8080** уже занят (например, веб-слой Cinema на `localhost:8080`), пробрось UI на **другой локальный порт**:

```bash
ssh -p 2315 -L 18080:127.0.0.1:8080 hl@hlssh.zil.digital
```

В браузере: **`http://127.0.0.1:18080`**. Конкретный **SSH-порт** (`2314` или `2315`) и пользователь смотри в **таблице ресурсов** и в `kafka-2026.md`.

### Доступ к брокеру с ПК (туннель)

Если в методичке нужно писать в Kafka **с локальной машины**, подними туннель на порт **bootstrap** брокера (в нашем проекте для приложения используется **9094** на `hl14`/`hl15`) — точная схема и порты в **§ про SSH-туннелирование** в `kafka-2026.md`. После туннеля в клиенте указывается `127.0.0.1:<локальный_порт>`.

---

## Лабораторная 12 — приложение и команды в Kafka

### Задача

`crud-app` **читает** из топика варианта JSON-сообщения (`entity`, `operation`, `payload`) и выполняет операции над фильмами, зрителями и билетами.

### Связка с hl14 / hl15 в Docker

Контейнер `crud-app` должен **резолвить** `hl14.zil` и `hl15.zil` (см. комментарий в `docker-compose.hl12.yml`). В compose заданы `extra_hosts` с IP из сети курса (по умолчанию `10.60.3.12` и `10.60.3.13`). Если адреса другие — задай `HL14_ZIL_IP` и `HL15_ZIL_IP` в `.env`.

Bootstrap в проекте: **`hl14.zil:9094`**, **`hl15.zil:9094`** (`KAFKA_BOOTSTRAP_SERVERS`).

### Топик

Как в таблице ресурсов; в репозитории по умолчанию задано **`hl03`**. Переопределение: `KAFKA_TOPIC` в environment или флаг `--topic` у скрипта.

### Код и настройки

- `src/main/java/ru/hse/lab2/kafka/*`
- `Lab2Application` — `@EnableKafka`
- `application.properties` — `spring.kafka.bootstrap-servers`, `lab.kafka.topic`

### Зависимость для скрипта

```bash
cd ~/lab2_rovnyagin
source .venv/bin/activate
python3 -m pip install kafka-python
```

### Шпаргалка для сдачи (на ВМ со стендом)

Контейнер должен видеть `hl14.zil` / `hl15.zil`.

**Шаг A — отправить команду в Kafka**

```bash
cd ~/lab2_rovnyagin
source .venv/bin/activate
```

```bash
TITLE="BUUUBUU$(date +%s)"
echo "$TITLE"
```

```bash
python scripts/send_kafka_message.py \
  --bootstrap-server hl14.zil:9094,hl15.zil:9094 \
  --entity FILM \
  --operation POST \
  --payload "{\"title\":\"$TITLE\",\"genre\":\"Demo\",\"durationMinutes\":120}"
```

При необходимости: `--topic <твой_топик>`.

**Шаг B — логи приложения**

```bash
docker logs lab8_crud_app 2>&1 | grep "Processed Kafka command" | tail -n 5
```

**Шаг C — результат в REST / БД**

```bash
curl -sS http://127.0.0.1:8080/api/films | python3 -m json.tool | grep -F "$TITLE"
```

### Подъём стенда

```bash
cd ~/lab2_rovnyagin
bash scripts/lab9-hl3.sh prepare
```

или вручную: `docker compose -f docker-compose.hl12.yml --env-file .env up -d --build`.

## Что приложить в отчёт

**Лаб. 11**

- что такое **hl14/hl15**, как заходил по SSH (порты из таблицы);
- кратко: просмотр Swarm/логов (по методичке);
- папка `/home/hl/<вариант>`;
- имя **топика** и строка в **Google Sheets**;
- скрин или описание **Kafka UI** (в т.ч. туннель, если **8080** занят — другой локальный порт).

**Лаб. 12**

- `extra_hosts`, bootstrap **9094**, имя топика;
- вывод шага B и шага C (фильм с `$TITLE` в `GET /api/films`).

## Быстрый чеклист

Поднять приложение на персональной ВМ:

```bash
cd ~/lab2_rovnyagin
bash scripts/lab9-hl3.sh prepare
```

Лаб. 11: туннель Kafka UI (пример):

```bash
ssh -p 2315 -L 18080:127.0.0.1:8080 hl@hlssh.zil.digital
```

Лаб. 12 — смок-тест Kafka:

```bash
cd ~/lab2_rovnyagin
source .venv/bin/activate
TITLE="BUUUBUU$(date +%s)"
python scripts/send_kafka_message.py \
  --bootstrap-server hl14.zil:9094,hl15.zil:9094 \
  --entity FILM --operation POST \
  --payload "{\"title\":\"$TITLE\",\"genre\":\"Demo\",\"durationMinutes\":120}"
docker logs lab8_crud_app 2>&1 | grep "Processed Kafka command" | tail -n 5
curl -sS http://127.0.0.1:8080/api/films | python3 -m json.tool | grep -F "$TITLE"
```
