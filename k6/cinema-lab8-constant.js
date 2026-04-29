import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

/**
 * LAB 8 — как LAB 6 server-server, но GET идёт на Additional service:
 *   POST → основной CRUD /api/films
 *   GET → Additional /api/analytics/max-viewers-by-film-title (RestTemplate→CRUD внутри)
 *
 * Переменные:
 *   BASE_URL_MAIN     — основной CRUD (8080)
 *   BASE_URL_ADDITIONAL — Additional (8081)
 *   TARGET_VUS, POST_SHARE, DURATION, K6_ROUTE
 *   FILM_TITLE — фильм из БД для GET (по умолчанию «Интерстеллар» из Flyway V2)
 *   LAB8_SUMMARY_FILE — JSON summary (run-lab8-ratio-sweep.sh)
 */
const baseMain = __ENV.BASE_URL_MAIN || 'http://localhost:8080';
const baseAdditional = __ENV.BASE_URL_ADDITIONAL || 'http://localhost:8081';
const filmTitle = __ENV.FILM_TITLE || 'Интерстеллар';
const postShare = Number(__ENV.POST_SHARE || '0.5');
const targetVus = Number(__ENV.TARGET_VUS || '30');
const duration = __ENV.DURATION || '90s';

const postFilmMs = new Trend('post_ms');
const getAnalyticsMs = new Trend('get_ms');

export const options = {
  vus: targetVus,
  duration,
  summaryTrendStats: ['avg', 'p(95)', 'min', 'med', 'max'],
  thresholds: {
    http_req_failed: ['rate<0.35'],
  },
};

const jsonHeaders = { headers: { 'Content-Type': 'application/json' } };

export default function () {
  if (Math.random() < postShare) {
    const title = `k6-L8-${__VU}-${__ITER}-${Date.now()}`;
    const body = JSON.stringify({
      title,
      genre: 'Lab8',
      durationMinutes: 100,
    });
    const res = http.post(`${baseMain.replace(/\/+$/, '')}/api/films`, body, jsonHeaders);
    postFilmMs.add(res.timings.duration);
    check(res, { 'POST 201': (r) => r.status === 201 });
  } else {
    const q = encodeURIComponent(filmTitle);
    const url = `${baseAdditional.replace(/\/+$/, '')}/api/analytics/max-viewers-by-film-title?filmTitle=${q}`;
    const res = http.get(url);
    getAnalyticsMs.add(res.timings.duration);
    check(res, { 'GET 200': (r) => r.status === 200 });
  }
  sleep(0.05);
}

export function handleSummary(data) {
  const outPath = __ENV.LAB8_SUMMARY_FILE;
  if (!outPath) {
    throw new Error(
      'LAB8_SUMMARY_FILE не задан — запускайте через k6/run-lab8-ratio-sweep.sh',
    );
  }
  const enriched = {
    ...data,
    lab6_meta: {
      target_vus: Number(__ENV.TARGET_VUS || '0'),
      duration: __ENV.DURATION || '',
      base_url: __ENV.BASE_URL_ADDITIONAL || '',
      base_url_main: __ENV.BASE_URL_MAIN || '',
      k6_route: __ENV.K6_ROUTE || 'server-to-server',
      post_share: Number(__ENV.POST_SHARE || '0'),
      film_id: String(__ENV.FILM_ID || '1'),
      film_title: String(__ENV.FILM_TITLE || ''),
      scenario: 'cinema-lab8-additional',
    },
  };
  return {
    [outPath]: JSON.stringify(enriched, null, 2),
  };
}
