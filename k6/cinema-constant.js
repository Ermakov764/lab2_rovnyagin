import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

/**
 * Constant-load scenario (LAB13 по умолчанию):
 *   POST -> kafka-proxy POST /produce/viewer -> Kafka -> crud-app batch listener
 *   GET  -> CRUD GET /api/cinema/films/max-viewers-summary
 *
 * Легаси (лаб. 12 стиль): K6_WRITE_MODE=rest — POST -> CRUD POST /api/viewers
 */
const baseMain = (__ENV.BASE_URL_MAIN || 'http://localhost:8080').replace(/\/+$/, '');
const baseKafkaProxy = (__ENV.BASE_URL_KAFKA_PROXY || 'http://127.0.0.1:8082').replace(/\/+$/, '');
const writeMode = (__ENV.K6_WRITE_MODE || 'kafka').toLowerCase();
const summaryLimit = __ENV.SUMMARY_LIMIT || '100';
const postShare = Number(__ENV.POST_SHARE || '0.5');
const targetVus = Number(__ENV.TARGET_VUS || '30');
const duration = __ENV.DURATION || '90s';
const httpTimeout = __ENV.K6_HTTP_TIMEOUT || '120s';
const failRateMax = Number(__ENV.K6_HTTP_FAIL_RATE_MAX || '0.35');
const thresholdsOff = __ENV.K6_THRESHOLDS_OFF === '1';

const postViewerMs = new Trend('post_ms');
const getSummaryMs = new Trend('get_ms');

const REQ_NAME_POST = 'Post';
const REQ_NAME_GET = 'Get';

const httpParams = { timeout: httpTimeout };

export const options = {
  vus: targetVus,
  duration,
  summaryTrendStats: ['avg', 'p(95)', 'min', 'med', 'max'],
  thresholds: thresholdsOff ? {} : { http_req_failed: [`rate<${failRateMax}`] },
};

const jsonHeaders = { headers: { 'Content-Type': 'application/json' }, ...httpParams };

export default function () {
  if (Math.random() < postShare) {
    const unique = `${__VU}-${__ITER}-${Date.now()}`;
    const viewerPayload = {
      name: `viewer-${unique}`,
      email: `viewer-${unique}@k6.local`,
    };
    let res;
    if (writeMode === 'rest') {
      const body = JSON.stringify(viewerPayload);
      res = http.post(`${baseMain}/api/viewers`, body, {
        ...jsonHeaders,
        tags: { name: REQ_NAME_POST },
      });
      postViewerMs.add(res.timings.duration);
      check(res, { 'POST viewer 201': (r) => r.status === 201 });
    } else {
      const body = JSON.stringify({ payload: viewerPayload });
      res = http.post(`${baseKafkaProxy}/produce/viewer`, body, {
        ...jsonHeaders,
        tags: { name: REQ_NAME_POST },
      });
      postViewerMs.add(res.timings.duration);
      check(res, { 'Kafka proxy POST 200': (r) => r.status === 200 });
    }
  } else {
    const url = `${baseMain}/api/cinema/films/max-viewers-summary?limit=${encodeURIComponent(summaryLimit)}`;
    const res = http.get(url, { ...httpParams, tags: { name: REQ_NAME_GET } });
    getSummaryMs.add(res.timings.duration);
    check(res, { 'GET summary 200': (r) => r.status === 200 });
  }
  sleep(0.05);
}

function trendHasAvg(t) {
  if (!t || typeof t !== 'object') return false;
  const v = t.values;
  if (v && typeof v === 'object' && typeof v.avg === 'number') return true;
  return typeof t.avg === 'number';
}

function shimTrendsFromHttpDuration(metrics) {
  if (!metrics || typeof metrics !== 'object') return metrics;
  const m = { ...metrics };
  const pick = (needle) => {
    const hit = Object.keys(m).find(
      (k) => k.startsWith('http_req_duration{') && k.includes(needle),
    );
    return hit ? m[hit] : null;
  };
  if (!trendHasAvg(m.get_ms)) {
    const sub = pick(`name:${REQ_NAME_GET}`);
    if (sub && trendHasAvg(sub)) m.get_ms = sub;
  }
  if (!trendHasAvg(m.post_ms)) {
    const sub = pick(`name:${REQ_NAME_POST}`);
    if (sub && trendHasAvg(sub)) m.post_ms = sub;
  }
  return m;
}

export function handleSummary(data) {
  const outPath = __ENV.SUMMARY_FILE;
  if (!outPath) {
    throw new Error('SUMMARY_FILE не задан — запускайте через k6/run-ratio-sweep.sh');
  }
  const metrics = shimTrendsFromHttpDuration(data.metrics);
  // Обязателен для k6/plot_k6_cpu_results.py (см. parse_lab6_meta): иначе графики не строятся.
  const lab6_meta = {
    target_vus: Number(__ENV.TARGET_VUS || '0'),
    duration: __ENV.DURATION || '',
    base_url: baseMain,
    k6_route: __ENV.K6_ROUTE || 'server-to-server',
  };
  const enriched = {
    ...data,
    metrics,
    lab6_meta,
    run_meta: {
      target_vus: Number(__ENV.TARGET_VUS || '0'),
      duration: __ENV.DURATION || '',
      base_url: __ENV.BASE_URL_MAIN || '',
      base_url_kafka_proxy: __ENV.BASE_URL_KAFKA_PROXY || '',
      k6_write_mode: writeMode,
      summary_limit: String(__ENV.SUMMARY_LIMIT || ''),
      post_share: Number(__ENV.POST_SHARE || '0'),
      k6_route: __ENV.K6_ROUTE || 'server-to-server',
      scenario:
        writeMode === 'rest'
          ? 'cinema-constant-viewers-rest-post-summary-get'
          : 'cinema-constant-kafka-proxy-viewer-post-summary-get',
    },
  };
  return {
    [outPath]: JSON.stringify(enriched, null, 2),
  };
}
