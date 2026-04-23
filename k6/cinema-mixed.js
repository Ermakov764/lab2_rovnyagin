import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * LAB4: смешанная нагрузка — POST «простой» сущности (Film без FK) и GET аналитики.
 * executor: ramping-vus, пакет k6/http.
 *
 * Переменные окружения:
 *   BASE_URL   — хост API (по умолчанию http://localhost:8080)
 *   TARGET_VUS — целевое число VU для сценария (задаётся run-sweep.sh)
 *   FILM_ID    — фильм для аналитики (из сида обычно 1)
 *   POST_SHARE — доля POST в [0..1], по умолчанию 0.5 (остальное — GET)
 */
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const filmId = __ENV.FILM_ID || '1';
const postShare = Number(__ENV.POST_SHARE || '0.5');
const targetVus = Number(__ENV.TARGET_VUS || '10');

export const options = {
  scenarios: {
    mixed: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: targetVus },
        { duration: '45s', target: targetVus },
        { duration: '15s', target: 0 },
      ],
      gracefulRampDown: '15s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.15'],
  },
};

const jsonHeaders = { headers: { 'Content-Type': 'application/json' } };

export default function () {
  if (Math.random() < postShare) {
    const title = `k6-film-${__VU}-${__ITER}-${Date.now()}`;
    const body = JSON.stringify({
      title,
      genre: 'LoadTest',
      durationMinutes: 120,
    });
    const res = http.post(`${baseUrl}/api/films`, body, jsonHeaders);
    check(res, { 'POST /api/films 201': (r) => r.status === 201 });
  } else {
    const url = `${baseUrl}/api/tickets/analytics/max-viewers?filmId=${filmId}`;
    const res = http.get(url);
    check(res, { 'GET analytics 200': (r) => r.status === 200 });
  }
  sleep(0.05);
}
