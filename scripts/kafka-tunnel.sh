#!/usr/bin/env bash
set -euo pipefail

SSH_HOST="${SSH_HOST:-hlssh.zil.digital}"
SSH_PORT="${SSH_PORT:-2315}"
SSH_USER="${SSH_USER:-hl}"

KAFKA_UI_LOCAL="${KAFKA_UI_LOCAL:-127.0.0.1:18080}"
KAFKA_UI_REMOTE="${KAFKA_UI_REMOTE:-127.0.0.1:8080}"
KAFKA_HL15_LOCAL="${KAFKA_HL15_LOCAL:-127.0.0.2:9094}"
KAFKA_HL15_REMOTE="${KAFKA_HL15_REMOTE:-127.0.0.1:9094}"
KAFKA_HL14_LOCAL="${KAFKA_HL14_LOCAL:-127.0.0.3:9094}"
KAFKA_HL14_REMOTE="${KAFKA_HL14_REMOTE:-10.60.3.12:9094}"

REMOTE="${SSH_USER}@${SSH_HOST}"

echo "Kafka UI: http://${KAFKA_UI_LOCAL}/"
echo "Kafka brokers: hl15.zil:9094, hl14.zil:9094"
echo "SSH: ${REMOTE} port ${SSH_PORT}"

exec ssh -p "${SSH_PORT}" \
  -o ExitOnForwardFailure=yes \
  -o ServerAliveInterval=30 \
  -o ServerAliveCountMax=3 \
  -N \
  -L "${KAFKA_UI_LOCAL}:${KAFKA_UI_REMOTE}" \
  -L "${KAFKA_HL15_LOCAL}:${KAFKA_HL15_REMOTE}" \
  -L "${KAFKA_HL14_LOCAL}:${KAFKA_HL14_REMOTE}" \
  "${REMOTE}"
