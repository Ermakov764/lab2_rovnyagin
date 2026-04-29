import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

/**
 * LAB 8 нагрузка:
 *   POST → основной CRUD POST /api/viewers (зрители с уникальным email)
 *   GET → основной CRUD GET /api/cinema/films/max-viewers-summary (сводка; Additional может тем же клиентом дёрнуть CRUD через RestTemplate на 8081)
 *
 * Переменные:
 *   BASE_URL_MAIN — CRUD (:8080)
 *   SUMMARY_LIMIT — limit для GET summary (по умолчанию 100)
 *   TARGET_VUS, POST_SHARE, DURATION
 *   LAB8_SUMMARY_FILE — JSON summary (run-lab8-ratio-sweep.sh)
 */
const baseMain = (__ENV.BASE_URL_MAIN || 'http://localhost:8080').replace(/\/+$/, '');
const summaryLimit = __ENV.SUMMARY_LIMIT || '100';
const postShare = Number(__ENV.POST_SHARE || '0.5');
const targetVus = Number(__ENV.TARGET_VUS || '30');
const duration = __ENV.DURATION || '90s';

const postViewerMs = new Trend('post_ms');
const getSummaryMs = new Trend('get_ms');

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
    const unique = `${__VU}-${__ITER}-${Date.now()}`;
    const body = JSON.stringify({
      name: `k6-L8-viewer-${unique}`,
      email: `k6-l8-${unique}@k6.local`,
    });
    const res = http.post(`${baseMain}/api/viewers`, body, jsonHeaders);
    postViewerMs.add(res.timings.duration);
    check(res, { 'POST viewer 201': (r) => r.status === 201 });
  } else {
    const url = `${baseMain}/api/cinema/films/max-viewers-summary?limit=${encodeURIComponent(summaryLimit)}`;
    const res = http.get(url);
    getSummaryMs.add(res.timings.duration);
    check(res, { 'GET summary 200': (r) => r.status === 200 });
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
      base_url: __ENV.BASE_URL_MAIN || '',
      summary_limit: String(__ENV.SUMMARY_LIMIT || ''),
      post_share: Number(__ENV.POST_SHARE || '0'),
      k6_route: __ENV.K6_ROUTE || 'server-to-server',
      scenario: 'cinema-lab8-viewers-post-summary-get',
    },
  };
  return {
    [outPath]: JSON.stringify(enriched, null, 2),
  };
}
