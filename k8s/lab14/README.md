# Лабораторная 14 — манифесты k3s / Kubernetes

## Локальная проверка (без кластера)

```bash
./gradlew build
./k8s/lab14/validate-manifests.sh
```

Скрипт проверяет, что все `*.yaml` в каталоге парсятся и в каждом документе есть `kind` и `apiVersion`. Полный `kubectl apply --dry-run=server` возможен только при доступе к API кластера.

Namespace **`hl3`**, образы по умолчанию `lavrentiyermakov/lab2_rovnyagin:latest` и `lavrentiyermakov/lab2_additional:latest` — при необходимости отредактируйте `05-deployment-additional.yaml` и `06-deployment-main.yaml`.

## Что уже в репозитории

| Файл | Назначение |
|------|------------|
| `00-namespace.yaml` | Namespace варианта |
| `01-configmap-main.yaml` | ENV основного сервиса (БД, Kafka по IP, внутренний URL доп.) |
| `02-configmap-additional.yaml` | ENV доп. сервиса (`MAIN_CRUD_BASE_URL` на ClusterIP основного) |
| `03-secret-db.yaml.example` | Шаблон пароля БД |
| `04-secret-dockerhub.yaml.example` | Инструкция для `docker-registry` Secret |
| `05-deployment-additional.yaml` | Deployment доп. сервиса (раньше основного — как в compose) |
| `06-deployment-main.yaml` | Deployment основного сервиса, **Guaranteed** 1 CPU / 1Gi |
| `07-services.yaml` | ClusterIP (внутри кластера) + **NodePort** 30303 / 30304 для Swagger |

## Порядок на кластере

1. Подключить свою ВМ к k3s по `K3S-SETUP.md` (разделы 5–6).
2. Создать секрет с паролем Postgres (значение как у курса для БД на `10.60.3.9`):

   ```bash
   kubectl create namespace hl3 --dry-run=client -o yaml | kubectl apply -f -
   kubectl -n hl3 create secret generic lab2-db-credentials \
     --from-literal=SPRING_DATASOURCE_PASSWORD='ВАШ_ПАРОЛЬ'
   ```

   Либо скопируйте `03-secret-db.yaml.example` в `03-secret-db.yaml`, подставьте пароль (файл в `.gitignore`) и выполните `kubectl apply -f k8s/lab14/03-secret-db.yaml`.

3. (Опционально) Приватные образы Docker Hub — создайте `dockerhub-credentials` по комментарию в `04-secret-dockerhub.yaml.example` и раскомментируйте `imagePullSecrets` в `05-deployment-additional.yaml` и `06-deployment-main.yaml`.

4. Применить манифесты:

   ```bash
   kubectl apply -f /path/to/repo/k8s/lab14/
   ```

   Файлы `*.yaml.example` kubectl не подхватывает.

5. Проверка:

   ```bash
   kubectl get pods,svc -n hl3 -o wide
   ```

## Демонстрация по ТЗ

- **Port-forward (основной):**  
  `kubectl -n hl3 port-forward svc/lab2-main-nodeport 8080:8080`  
  затем в браузере: **`http://localhost:8080/swagger-ui/index.html`** (или `/swagger-ui.html`, если редиректит).

- **Port-forward (доп.):**  
  `kubectl -n hl3 port-forward svc/lab2-additional-nodeport 8081:8081` → **`http://localhost:8081/swagger-ui/index.html`**

- **NodePort:** с машины в сети VPN к IP **ноды**, на которой слушает kube-proxy (часто IP вашей ВМ или мастера):  
  - основной: `http://<NODE_IP>:30303/swagger-ui/index.html`  
  - дополнительный: `http://<NODE_IP>:30304/swagger-ui/index.html`

При занятости портов **30303** / **30304** измените `nodePort` в `07-services.yaml`.

## Внутренняя связность (HTTP)

- Доп. сервис → основной: `http://lab2-main:8080` (ClusterIP).
- Основной → доп.: `http://lab2-additional:8081` (ClusterIP).

Не используйте `https://` для этих URL внутри кластера, если контейнеры не поднимают TLS.
