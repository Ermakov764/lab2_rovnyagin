package ru.hse.lab2.observability;

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

// Наблюдаемость: события кладём в очередь, раз в tick пересчитываем скользящие окна (10s / 30s / 1m из конфига).
@Service
public class ObservabilityService {

    private static final Logger log = LoggerFactory.getLogger(ObservabilityService.class);

    // Журнал завершённых операций (потокобезопасная очередь).
    private final ConcurrentLinkedQueue<TimedEvent> events = new ConcurrentLinkedQueue<>();
    // То, что отдаёт REST: последний полный пересчёт всех окон → операции → цифры.
    private final AtomicReference<Map<String, Map<String, OperationStats>>> snapshots;
    private final List<WindowConfig> windows;
    // Самое длинное окно — чтобы выкидывать из очереди всё старее (ни одно окно уже не охватит).
    private final long maxWindowMs;
    private final String applicationName;
    private final boolean logOnRefresh;
    private final boolean logEmptySnapshots;

    public ObservabilityService(
            @Value("${observability.windows:10s,30s,1m}") String windowsConfig,
            @Value("${spring.application.name:lab2-main-service}") String applicationName,
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

    // Засечка для замера длительности — парой с nanoTime в stop*.
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

    // Раз в observability.tick-ms: подчистить память + пересобрать статистику по каждому окну.
    @Scheduled(fixedDelayString = "${observability.tick-ms:1000}")
    public void refresh() {
        long nowMs = System.currentTimeMillis();
        long threshold = nowMs - maxWindowMs;
        // Убираем с начала очереди события старше самого длинного окна — они нигде не нужны.
        while (true) {
            TimedEvent head = events.peek();
            if (head == null || head.atMs() >= threshold) {
                break;
            }
            events.poll();
        }

        // Снимок на момент тика; параллельно новые record() могут уже дописывать в events.
        List<TimedEvent> current = new ArrayList<>(events);
        Map<String, Map<String, OperationStats>> computed = new LinkedHashMap<>();
        for (WindowConfig window : windows) {
            computed.put(window.label(), aggregateWindow(current, nowMs, window.windowMs()));
        }
        snapshots.set(computed);
        maybeLog(computed);
    }

    // Время жизни операции — nanoTime; попадание в «календарное» окно — по currentTimeMillis при записи.
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

    /**
     * Логгирует снимок метрик, если включен логгинг и выполняются условия (например, наличие событий).
     * @param computed Снимок метрик по всем окнам
     */
    private void maybeLog(Map<String, Map<String, OperationStats>> computed) {
        // Если логгирование выключено или уровень info не включён — ничего не делаем
        if (!logOnRefresh || !log.isInfoEnabled()) {
            return;
        }
        // Если запрещено логгировать пустые снимки и текущий снимок пуст — не логгируем
        if (!logEmptySnapshots && isEmpty(computed)) {
            return;
        }
        // Логгируем метрики по приложению и окнам в формате, пригодном для чтения
        log.info("[{}] observability refresh\n{}", applicationName, toReadable(computed));
    }

    // Ни в одном окне нет операций с хотя бы одним событием.
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

    /**
     * Преобразует снимок метрик по окнам и операциям в читаемую строку для логирования или REST.
     * 
     * @param snapshot Карта: имя окна -> карта метрик по операциям
     * @return Многострочная строка с форматированной статистикой по каждому окну и операции
     */
    private static String toReadable(Map<String, Map<String, OperationStats>> snapshot) {
        StringBuilder sb = new StringBuilder(); // Аккумулируем вывод в StringBuilder
        // Проходим по всем окнам (например, "10s", "1m" и т.д.)
        for (Map.Entry<String, Map<String, OperationStats>> entry : snapshot.entrySet()) {
            sb.append("window ").append(entry.getKey()).append('\n'); // Добавляем заголовок окна
            // Если для данного окна не было операций — пишем, что событий нет
            if (entry.getValue().isEmpty()) {
                sb.append("  (no events)\n"); // Помечаем отсутствие событий для окна
                continue;
            }
 
            entry.getValue().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(op -> {
                        OperationStats s = op.getValue();
                        // Форматируем строку с метриками для операции:
                        // n — количество событий
                        // err — количество ошибок
                        // errRate — процент ошибок
                        // avg — среднее время (мс)
                        // min — минимальное время (мс)
                        // max — максимальное время (мс)
                        // p50, p95, p99 — медиана, 95-й и 99-й перцентили (мс)
                        // rps — среднее число событий в секунду для данного окна
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

    // Окно [nowMs - windowMs, nowMs]: фильтр по atMs, группировка по имени операции, метрики по длительности.
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

        // Создаём мапу для хранения статистики по каждой операции в данном окне
        Map<String, OperationStats> result = new LinkedHashMap<>();
        // Длина окна в секундах — нужна для расчёта rps (запросов/событий в секунду)
        double seconds = windowMs / 1000.0;
        // Для каждой операции, которую наблюдали в окне
        for (Map.Entry<String, Bucket> entry : buckets.entrySet()) {
            Bucket b = entry.getValue();
            // Сортируем длительности всех событий по возрастанию
            double[] sorted = b.durationsMs.stream().mapToDouble(Double::doubleValue).sorted().toArray();
            // Суммируем все длительности для вычисления среднего времени
            double sum = Arrays.stream(sorted).sum();
            // Средняя длительность события (в миллисекундах)
            double avg = sum / sorted.length;
            // Минимальная длительность события
            double min = sorted[0];
            // Максимальная длительность события
            double max = sorted[sorted.length - 1];
            // Медиана (50-й перцентиль)
            double p50 = percentile(sorted, 0.50);
            // 95-й перцентиль (95% событий были быстрее этого значения)
            double p95 = percentile(sorted, 0.95);
            // 99-й перцентиль (99% событий были быстрее этого значения)
            double p99 = percentile(sorted, 0.99);
            // Процент ошибок среди всех событий: если событий не было — 0, иначе вычисляем долю ошибок
            double errorRate = b.count == 0 ? 0.0 : (b.errors * 100.0) / b.count;
            // rps — среднее число событий в секунду за интервал окна
            // Если окно имеет нулевую длину, rps = 0 чтобы избежать деления на ноль
            double rps = seconds == 0 ? 0.0 : b.count / seconds;
            // Кладём вычисленную статистику по операции в результат
            result.put(entry.getKey(), new OperationStats(
                    b.count,          // количество событий
                    b.errors,         // количество ошибок
                    errorRate,        // процент ошибок
                    avg,              // среднее время выполнения (мс)
                    min,              // минимальное время (мс)
                    max,              // максимальное время (мс)
                    p50,              // медиана (мс)
                    p95,              // 95-й перцентиль (мс)
                    p99,              // 99-й перцентиль (мс)
                    rps               // событий в секунду (rps)
            ));
        }
        // Возвращаем итоговую мапу: имя операции → статистика по окну времени
        return result;
    }

    // Перцентиль по уже отсортированному массиву длительностей (мс).
    /**
     * Вычисляет q-й перцентиль (например, 0.5 — медиана, 0.95 — 95-й перцентиль) по уже отсортированному массиву.
     * Массив должен быть отсортирован по возрастанию.
     *
     * @param sorted массив значений, отсортированный по возрастанию
     * @param q      значение перцентиля в диапазоне [0, 1] (например, 0.5 — медиана)
     * @return       значение q-го перцентиля
     */
    private static double percentile(double[] sorted, double q) {
        // Если массив пустой — возвращаем 0
        if (sorted.length == 0) {
            return 0.0;
        }
        // Если в массиве единственный элемент — возвращаем его
        if (sorted.length == 1) {
            return sorted[0];
        }
        // Место, соответствующее нужному перцентилю в массиве (линейный интерполятор)
        double pos = q * (sorted.length - 1);
        int lo = (int) Math.floor(pos);
        int hi = (int) Math.ceil(pos);
        if (lo == hi) {
            // Если pos — целое, просто возвращаем значение в этой позиции
            return sorted[lo];
        }
        // Если pos между двумя целыми — интерполируем между ними
        double fraction = pos - lo;
        return sorted[lo] + (sorted[hi] - sorted[lo]) * fraction;
    }

    // Строка конфига вида "10s,30s,1m".
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

    // Суффиксы ms | s | m → миллисекунды длины окна.
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

    // atMs — «когда записали»; durationNanos — как долго длилась операция (nanoTime).
    private record TimedEvent(long atMs, String operation, boolean failed, long durationNanos) {}

    // label — ключ в JSON (10s и т.д.); windowMs — сколько миллисекунд назад от now граница окна.
    private record WindowConfig(String label, long windowMs) {}

    // Промежуточный набор при подсчёте одного временного окна.
    private static final class Bucket {
        long count;
        long errors;
        final List<Double> durationsMs = new ArrayList<>();
    }

    // Готовая строка для REST/логов по одной операции внутри окна.
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
