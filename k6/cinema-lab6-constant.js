import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

// Метрики как в zil/k6 (plot_k6_reports.py)
const postFilmMs = new Trend('post_ms');
const getAnalyticsMs = new Trend('get_ms');

/**
 * LAB6 — модель как у одногруппницы (load.js + LAB6_CONST=1):
 * одна группа постоянных VU, в каждой итерации с вероятностью POST_SHARE — POST, иначе GET.
 * Эндпоинты кинотеатра: POST /api/films, GET /api/tickets/analytics/max-viewers
 *
 * Переменные окружения:
 *   BASE_URL, TARGET_VUS, POST_SHARE, DURATION, FILM_ID, K6_ROUTE
 *   LAB6_SUMMARY_FILE — путь JSON (run-lab6-ratio-sweep.sh)
 */
const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const filmId = __ENV.FILM_ID || '1';
const postShare = Number(__ENV.POST_SHARE || '0.5');
const targetVus = Number(__ENV.TARGET_VUS || '30');
const duration = __ENV.DURATION || '90s';

export const options = {
  vus: targetVus,
  duration,
  summaryTrendStats: ['avg', 'p(95)', 'min', 'med', 'max'],
  thresholds: {
    // На учебных ВМ под тяжёлым VU порог часто рвётся; sweep может задать K6_NO_THRESHOLDS=1
    http_req_failed: ['rate<0.35'],
  },
};

const jsonHeaders = { headers: { 'Content-Type': 'application/json' } };

export default function () {
  if (Math.random() < postShare) {
    const title = `k6-L6-${__VU}-${__ITER}-${Date.now()}`;
    const body = JSON.stringify({
      title,
      genre: 'Lab6',
      durationMinutes: 100,
    });
    const res = http.post(`${baseUrl}/api/films`, body, jsonHeaders); // Создаем фильм
    postFilmMs.add(res.timings.duration);                             // POST/api/films
    check(res, { 'POST 201': (r) => r.status === 201 });
  } else {
    const url = `${baseUrl}/api/tickets/analytics/max-viewers?filmId=${filmId}`; // GET — аналитика по билетам для фильм
    const res = http.get(url);                                       // GET/api/tickets/analytics/max-viewers?filmId=...
    getAnalyticsMs.add(res.timings.duration);
    check(res, { 'GET 200': (r) => r.status === 200 });
  }
  sleep(0.05);
}

export function handleSummary(data) {
  const outPath = __ENV.LAB6_SUMMARY_FILE;
  if (!outPath) {
    throw new Error(
      'LAB6_SUMMARY_FILE не задан — запускайте через k6/run-lab6-ratio-sweep.sh (или -e LAB6_SUMMARY_FILE=...).',
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
      scenario: 'cinema-lab6-zil',
    },
  };
  return {
    [outPath]: JSON.stringify(enriched, null, 2),
  };
}
