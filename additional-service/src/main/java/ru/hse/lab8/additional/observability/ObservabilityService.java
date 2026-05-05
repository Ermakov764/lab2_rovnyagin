package ru.hse.lab8.additional.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ObservabilityService {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityService.class);

    private final ConcurrentLinkedQueue<TimedEvent> events = new ConcurrentLinkedQueue<>();
    private final AtomicReference<Map<String, Map<String, OperationStats>>> snapshots;
    private final List<WindowConfig> windows;
    private final long maxWindowMs;
    private final String applicationName;
    private final boolean logOnRefresh;
    private final boolean logEmptySnapshots;

    public ObservabilityService(
            @Value("${observability.windows:10s,30s,1m}") String windowsConfig,
            @Value("${spring.application.name:lab8-additional-service}") String applicationName,
            @Value("${observability.log-on-refresh:true}") boolean logOnRefresh,
            @Value("${observability.log-empty-snapshots:false}") boolean logEmptySnapshots
    ) {
        this.windows = parseWindows(windowsConfig);
        this.maxWindowMs = windows.stream().mapToLong(WindowConfig::windowMs).max().orElse(60_000L);
        this.applicationName = applicationName;
        this.logOnRefresh = logOnRefresh;
        this.logEmptySnapshots = logEmptySnapshots;
        this.snapshots = new AtomicReference<>(emptySnapshot());
    }

    public long start() {
        return System.nanoTime();
    }

    public void stopSuccess(String operation, long startedAtNanos) {
        record(operation, startedAtNanos, false);
    }

    public void stopFailure(String operation, long startedAtNanos) {
        record(operation, startedAtNanos, true);
    }

    public Map<String, Map<String, OperationStats>> getAllWindows() {
        return snapshots.get();
    }

    public Map<String, OperationStats> getWindow(String window) {
        Map<String, OperationStats> stats = snapshots.get().get(window);
        if (stats == null) {
            throw new IllegalArgumentException("Unknown observability window: " + window);
        }
        return stats;
    }

    @Scheduled(fixedDelayString = "${observability.tick-ms:1000}")
    public void refresh() {
        long nowMs = System.currentTimeMillis();
        long threshold = nowMs - maxWindowMs;
        while (true) {
            TimedEvent head = events.peek();
            if (head == null || head.atMs() >= threshold) {
                break;
            }
            events.poll();
        }

        List<TimedEvent> current = new ArrayList<>(events);
        Map<String, Map<String, OperationStats>> computed = new LinkedHashMap<>();
        for (WindowConfig window : windows) {
            computed.put(window.label(), aggregateWindow(current, nowMs, window.windowMs()));
        }
        snapshots.set(computed);
        maybeLog(computed);
    }

    private void record(String operation, long startedAtNanos, boolean failed) {
        long durationNanos = Math.max(0L, System.nanoTime() - startedAtNanos);
        events.add(new TimedEvent(System.currentTimeMillis(), operation, failed, durationNanos));
    }

    private Map<String, Map<String, OperationStats>> emptySnapshot() {
        Map<String, Map<String, OperationStats>> empty = new LinkedHashMap<>();
        for (WindowConfig window : windows) {
            empty.put(window.label(), Map.of());
        }
        return empty;
    }

    private void maybeLog(Map<String, Map<String, OperationStats>> computed) {
        if (!logOnRefresh || !log.isInfoEnabled()) {
            return;
        }
        if (!logEmptySnapshots && isEmpty(computed)) {
            return;
        }
        log.info("[{}] observability refresh\n{}", applicationName, toReadable(computed));
    }

    private static boolean isEmpty(Map<String, Map<String, OperationStats>> snapshot) {
        for (Map<String, OperationStats> perWindow : snapshot.values()) {
            for (OperationStats stats : perWindow.values()) {
                if (stats.count() > 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String toReadable(Map<String, Map<String, OperationStats>> snapshot) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, OperationStats>> entry : snapshot.entrySet()) {
            sb.append("window ").append(entry.getKey()).append('\n');
            if (entry.getValue().isEmpty()) {
                sb.append("  (no events)\n");
                continue;
            }
            entry.getValue().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(op -> {
                        OperationStats s = op.getValue();
                        sb.append(String.format(Locale.ROOT,
                                "  %-45s n=%5d err=%4d errRate=%6.2f%% avg=%8.2f min=%8.2f max=%8.2f p50=%8.2f p95=%8.2f p99=%8.2f rps=%7.2f%n",
                                op.getKey(),
                                s.count(),
                                s.errors(),
                                s.errorRate(),
                                s.avgMs(),
                                s.minMs(),
                                s.maxMs(),
                                s.p50Ms(),
                                s.p95Ms(),
                                s.p99Ms(),
                                s.rps()));
                    });
        }
        return sb.toString();
    }

    private static Map<String, OperationStats> aggregateWindow(List<TimedEvent> current, long nowMs, long windowMs) {
        long fromMs = nowMs - windowMs;
        Map<String, Bucket> buckets = new LinkedHashMap<>();
        for (TimedEvent event : current) {
            if (event.atMs() < fromMs) {
                continue;
            }
            Bucket bucket = buckets.computeIfAbsent(event.operation(), ignored -> new Bucket());
            bucket.count++;
            if (event.failed()) {
                bucket.errors++;
            }
            bucket.durationsMs.add(event.durationNanos() / 1_000_000.0);
        }

        Map<String, OperationStats> result = new LinkedHashMap<>();
        double seconds = windowMs / 1000.0;
        for (Map.Entry<String, Bucket> entry : buckets.entrySet()) {
            Bucket b = entry.getValue();
            double[] sorted = b.durationsMs.stream().mapToDouble(Double::doubleValue).sorted().toArray();
            double sum = Arrays.stream(sorted).sum();
            double avg = sum / sorted.length;
            double min = sorted[0];
            double max = sorted[sorted.length - 1];
            double p50 = percentile(sorted, 0.50);
            double p95 = percentile(sorted, 0.95);
            double p99 = percentile(sorted, 0.99);
            double errorRate = b.count == 0 ? 0.0 : (b.errors * 100.0) / b.count;
            double rps = seconds == 0 ? 0.0 : b.count / seconds;
            result.put(entry.getKey(), new OperationStats(
                    b.count, b.errors, errorRate, avg, min, max, p50, p95, p99, rps
            ));
        }
        return result;
    }

    private static double percentile(double[] sorted, double q) {
        if (sorted.length == 0) {
            return 0.0;
        }
        if (sorted.length == 1) {
            return sorted[0];
        }
        double pos = q * (sorted.length - 1);
        int lo = (int) Math.floor(pos);
        int hi = (int) Math.ceil(pos);
        if (lo == hi) {
            return sorted[lo];
        }
        double fraction = pos - lo;
        return sorted[lo] + (sorted[hi] - sorted[lo]) * fraction;
    }

    private static List<WindowConfig> parseWindows(String rawConfig) {
        List<WindowConfig> parsed = new ArrayList<>();
        for (String part : rawConfig.split(",")) {
            String normalized = part.trim().toLowerCase(Locale.ROOT);
            if (normalized.isBlank()) {
                continue;
            }
            parsed.add(new WindowConfig(normalized, parseDurationMs(normalized)));
        }
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("observability.windows must not be empty");
        }
        return parsed;
    }

    private static long parseDurationMs(String raw) {
        Objects.requireNonNull(raw, "window");
        if (raw.endsWith("ms")) {
            return Long.parseLong(raw.substring(0, raw.length() - 2));
        }
        if (raw.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(raw.substring(0, raw.length() - 1))).toMillis();
        }
        if (raw.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(raw.substring(0, raw.length() - 1))).toMillis();
        }
        throw new IllegalArgumentException("Unsupported window duration: " + raw);
    }

    private record TimedEvent(long atMs, String operation, boolean failed, long durationNanos) {}

    private record WindowConfig(String label, long windowMs) {}

    private static final class Bucket {
        long count;
        long errors;
        final List<Double> durationsMs = new ArrayList<>();
    }

    public record OperationStats(
            long count,
            long errors,
            double errorRate,
            double avgMs,
            double minMs,
            double maxMs,
            double p50Ms,
            double p95Ms,
            double p99Ms,
            double rps
    ) {}
}
