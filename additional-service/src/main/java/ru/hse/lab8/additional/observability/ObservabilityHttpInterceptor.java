package ru.hse.lab8.additional.observability;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

public class ObservabilityHttpInterceptor implements HandlerInterceptor {

    private static final String ATTR_STARTED = "observability.startedNanos";

    private final ObservabilityService observabilityService;

    public ObservabilityHttpInterceptor(ObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        request.setAttribute(ATTR_STARTED, observabilityService.start());
        return true;
    }

    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            Exception ex
    ) {
        Object startedRaw = request.getAttribute(ATTR_STARTED);
        if (!(startedRaw instanceof Long started)) {
            return;
        }
        String operation = resolveOperationName(handler);
        if (ex != null || response.getStatus() >= 500) {
            observabilityService.stopFailure(operation, started);
            return;
        }
        observabilityService.stopSuccess(operation, started);
    }

    private static String resolveOperationName(Object handler) {
        if (handler instanceof HandlerMethod method) {
            return "controller." + method.getBeanType().getSimpleName() + "." + method.getMethod().getName();
        }
        return "controller.unknown";
    }
}
