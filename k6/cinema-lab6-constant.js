import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const postFilmMs = new Trend('k6_post_film_ms');
const getAnalyticsMs = new Trend('k6_get_analytics_ms');

/**
 * LAB6: постоянная нагрузка (constant VUs), два параллельных пула POST / GET.
 * Соотношение вставка/чтение задаётся POST_SHARE (доля VU под POST): например 0.05 → 5/95.
 *
 * Переменные окружения:
 *   BASE_URL   — API (по умолчанию http://localhost:8080)
 *   TARGET_VUS — суммарное число VU (const)
 *   POST_SHARE — доля VU под POST в [0..1]
 *   DURATION   — длительность ступени с постоянными VU (по умолчанию 90s)
 *   FILM_ID    — для GET /api/tickets/analytics/max-viewers
 *   LAB6_SUMMARY_FILE — путь JSON (задаёт run-lab6-ratio-sweep.sh)
 *   K6_ROUTE   — pc-to-server | server-to-server (для подписи графиков / отчёта)
 */
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const filmId = __ENV.FILM_ID || '1';
const postShare = Number(__ENV.POST_SHARE || '0.5');
const targetVus = Number(__ENV.TARGET_VUS || '30');
const duration = __ENV.DURATION || '90s';

// Округление долей; при малых TARGET_VUS гарантируем хотя бы 1 VU в «нужной» стороне
let postVuPool = Math.min(targetVus, Math.round(targetVus * postShare));
let getVuPool = targetVus - postVuPool;
if (targetVus > 0 && postShare > 0 && postVuPool === 0) {
  postVuPool = 1;
  getVuPool = targetVus - 1;
}
if (targetVus > 0 && postShare < 1 && getVuPool === 0) {
  getVuPool = 1;
  postVuPool = targetVus - 1;
}

const scenarios = {};
if (postVuPool > 0) {
  scenarios.post_films = {
    executor: 'constant-vus',
    vus: postVuPool,
    duration,
    exec: 'postFilms',
    startTime: '0s',
  };
}
if (getVuPool > 0) {
  scenarios.get_analytics = {
    executor: 'constant-vus',
    vus: getVuPool,
    duration,
    exec: 'getAnalytics',
    startTime: '0s',
  };
}

export const options = {
  scenarios,
  thresholds: {
    http_req_failed: ['rate<0.20'],
  },
};

const jsonHeaders = { headers: { 'Content-Type': 'application/json' } };

export function postFilms() {
  if (postVuPool <= 0) {
    return;
  }
  const title = `k6-L6-${__VU}-${__ITER}-${Date.now()}`;
  const body = JSON.stringify({
    title,
    genre: 'Lab6',
    durationMinutes: 100,
  });
  const res = http.post(`${baseUrl}/api/films`, body, jsonHeaders);
  postFilmMs.add(res.timings.duration);
  check(res, { 'POST 201': (r) => r.status === 201 });
  sleep(0.05);
}

export function getAnalytics() {
  if (getVuPool <= 0) {
    return;
  }
  const url = `${baseUrl}/api/tickets/analytics/max-viewers?filmId=${filmId}`;
  const res = http.get(url);
  getAnalyticsMs.add(res.timings.duration);
  check(res, { 'GET 200': (r) => r.status === 200 });
  sleep(0.05);
}

/**
 * Пишем summary JSON с блоком lab6_meta (TARGET_VUS, DURATION, BASE_URL, K6_ROUTE)
 * для подписей plot_lab6_from_results.py и единой серии прогонов.
 */
export function handleSummary(data) {
  const outPath = __ENV.LAB6_SUMMARY_FILE;
  if (!outPath) {
    throw new Error(
      'LAB6_SUMMARY_FILE не задан — запускайте только через k6/run-lab6-ratio-sweep.sh (или передайте -e LAB6_SUMMARY_FILE=...).',
    );
  }
  const enriched = {
    ...data,
    lab6_meta: {
      target_vus: Number(__ENV.TARGET_VUS || '0'),
      duration: __ENV.DURATION || '',
      base_url: __ENV.BASE_URL || '',
      k6_route: __ENV.K6_ROUTE || 'pc-to-server',
      post_share: Number(__ENV.POST_SHARE || '0'),
      film_id: String(__ENV.FILM_ID || '1'),
      scenario: 'cinema-lab6-constant',
    },
  };
  return {
    [outPath]: JSON.stringify(enriched, null, 2),
  };
}
