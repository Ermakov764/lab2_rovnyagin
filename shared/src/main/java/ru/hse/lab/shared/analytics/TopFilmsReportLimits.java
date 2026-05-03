package ru.hse.lab.shared.analytics;

/**
 * Ограничения query-параметра {@code limit} для отчётов «топ фильмов по билетам».
 * Используются основным CRUD и микросервисом additional-service.
 */
public final class TopFilmsReportLimits {

    public static final int MIN = 1;
    public static final int MAX = 50_000;
    public static final int DEFAULT = 1_000;

    private TopFilmsReportLimits() {
    }

    public static int effectiveLimit(Integer limitOrNull) {
        return limitOrNull != null ? limitOrNull : DEFAULT;
    }

    /**
     * @throws IllegalArgumentException если {@code limit} вне {@link #MIN}…{@link #MAX}
     */
    public static void requireInRange(int limit) {
        if (limit < MIN || limit > MAX) {
            throw new IllegalArgumentException("limit must be between " + MIN + " and " + MAX);
        }
    }
}
