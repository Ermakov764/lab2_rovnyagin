package ru.hse.lab8.additional.observability;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/observability")
public class ObservabilityController {

    private final ObservabilityService observabilityService;

    public ObservabilityController(ObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    @GetMapping
    public Object getStats(@RequestParam(required = false) String window) {
        if (window == null || window.isBlank()) {
            return observabilityService.getAllWindows();
        }
        return observabilityService.getWindow(window);
    }

    @GetMapping("/windows")
    public Map<String, Map<String, ObservabilityService.OperationStats>> getAllWindows() {
        return observabilityService.getAllWindows();
    }
}
