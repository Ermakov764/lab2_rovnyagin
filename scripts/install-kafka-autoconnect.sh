#!/usr/bin/env bash
set -euo pipefail

SSH_HOST="${SSH_HOST:-hlssh.zil.digital}"
SSH_PORT="${SSH_PORT:-2315}"
SSH_USER="${SSH_USER:-hl}"
SERVICE_NAME="${SERVICE_NAME:-lab2-kafka-tunnel}"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
TUNNEL_SCRIPT="${SCRIPT_DIR}/kafka-tunnel.sh"
SYSTEMD_USER_DIR="${HOME}/.config/systemd/user"
SERVICE_FILE="${SYSTEMD_USER_DIR}/${SERVICE_NAME}.service"

echo "1/4 Добавляю loopback-адреса 127.0.0.2 и 127.0.0.3"
sudo ip addr add 127.0.0.2/8 dev lo 2>/dev/null || true
sudo ip addr add 127.0.0.3/8 dev lo 2>/dev/null || true

echo "2/4 Проверяю /etc/hosts"
ensure_host() {
  local ip="$1"
  local host="$2"
  if grep -Eq "^[[:space:]]*${ip}[[:space:]]+.*\\b${host}\\b" /etc/hosts; then
    return
  fi
  echo "${ip} ${host}" | sudo tee -a /etc/hosts >/dev/null
}
ensure_host "127.0.0.2" "hl15.zil"
ensure_host "127.0.0.3" "hl14.zil"

echo "3/4 Создаю systemd user-service ${SERVICE_NAME}"
mkdir -p "${SYSTEMD_USER_DIR}"
cat > "${SERVICE_FILE}" <<EOF
[Unit]
Description=Lab2 Kafka SSH tunnel
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
Environment=SSH_HOST=${SSH_HOST}
Environment=SSH_PORT=${SSH_PORT}
Environment=SSH_USER=${SSH_USER}
ExecStart=${TUNNEL_SCRIPT}
Restart=always
RestartSec=5

[Install]
WantedBy=default.target
EOF

chmod +x "${TUNNEL_SCRIPT}"

echo "4/4 Включаю и запускаю автотуннель"
systemctl --user daemon-reload
systemctl --user enable --now "${SERVICE_NAME}.service"

echo ""
echo "Готово. Kafka будет доступна так:"
echo "  UI: http://127.0.0.1:18080/"
echo "  bootstrap-server: hl15.zil:9094,hl14.zil:9094"
echo ""
echo "Проверить статус:"
echo "  systemctl --user status ${SERVICE_NAME}.service"
echo ""
echo "Посмотреть логи:"
echo "  journalctl --user -u ${SERVICE_NAME}.service -f"
