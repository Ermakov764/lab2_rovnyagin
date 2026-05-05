package ru.hse.lab2.observability;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Только отдаёт последний снимок из ObservabilityService — пересчёт делает refresh() по расписанию.
@RestController
@RequestMapping("/api/observability")
public class ObservabilityController {

    private final ObservabilityService observabilityService;

    public ObservabilityController(ObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    // GET /api/observability — все окна; ?window=30s — одно окно (ключ как в конфиге observability.windows).
    @GetMapping
    public Object getStats(@RequestParam(required = false) String window) {
        if (window == null || window.isBlank()) {
            return observabilityService.getAllWindows();
        }
        return observabilityService.getWindow(window);
    }

    // Явный URL для «все окна сразу», то же тело ответа что и GET без параметра.
    @GetMapping("/windows")
    public Map<String, Map<String, ObservabilityService.OperationStats>> getAllWindows() {
        return observabilityService.getAllWindows();
    }
}
