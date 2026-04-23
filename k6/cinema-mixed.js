import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

/** Попадают в summary-export отдельно — для графика POST vs GET в plot_avg_vs_vus.py */
const postFilmMs = new Trend('k6_post_film_ms');
const getAnalyticsMs = new Trend('k6_get_analytics_ms');

/**
 * LAB4: смешанная нагрузка — два параллельных пула VU:
 *   • post_films  — только POST /api/films
 *   • get_analytics — только GET аналитики
 * Оба используют ramping-vus с одинаковыми по форме стадиями, но разным целевым числом VU.
 *
 * Переменные окружения:
 *   BASE_URL    — хост API (по умолчанию http://localhost:8080)
 *   TARGET_VUS  — суммарное целевое число VU (задаётся run-sweep.sh)
 *   POST_SHARE  — доля VU под POST в [0..1], остальное — GET (по умолчанию 0.5)
 *   FILM_ID     — фильм для аналитики (в БД должны быть билеты на этот фильм)
 */
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const filmId = __ENV.FILM_ID || '1';
const postShare = Number(__ENV.POST_SHARE || '0.5');
const targetVus = Number(__ENV.TARGET_VUS || '10');

// Целочисленное разбиение: postVuPool + getVuPool === targetVus 
const postVuPool = Math.min(targetVus, Math.floor(targetVus * postShare));
const getVuPool = targetVus - postVuPool;

const rampStages = (peak) => [
  { duration: '15s', target: peak },
  { duration: '45s', target: peak },
  { duration: '15s', target: 0 },
];

export const options = {
  scenarios: {
    post_films: {
      executor: 'ramping-vus',
      exec: 'postFilms',
      startVUs: 0,
      stages: rampStages(postVuPool),
      gracefulRampDown: '15s',
    },
    get_analytics: {
      executor: 'ramping-vus',
      exec: 'getAnalytics',
      startVUs: 0,
      stages: rampStages(getVuPool),
      gracefulRampDown: '15s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.15'],
  },
};

const jsonHeaders = { headers: { 'Content-Type': 'application/json' } };

/** Пул 1: только создание фильмов */
export function postFilms() {
  const title = `k6-film-${__VU}-${__ITER}-${Date.now()}`;
  const body = JSON.stringify({
    title,
    genre: 'LoadTest',
    durationMinutes: 120,
  });
  const res = http.post(`${baseUrl}/api/films`, body, jsonHeaders);
  postFilmMs.add(res.timings.duration);
  check(res, { 'POST /api/films 201': (r) => r.status === 201 });
  sleep(0.05);
}

/** Пул 2: только аналитика */
export function getAnalytics() {
  const url = `${baseUrl}/api/tickets/analytics/max-viewers?filmId=${filmId}`;
  const res = http.get(url);
  getAnalyticsMs.add(res.timings.duration);
  check(res, { 'GET analytics 200': (r) => r.status === 200 });
  sleep(0.05);
}
