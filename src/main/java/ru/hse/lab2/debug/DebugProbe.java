package ru.hse.lab2.debug;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class DebugProbe {

    private static final String SESSION_ID = "0da112";
    private static final Path LOG_PATH = Path.of("/home/serafim/Desktop/Labs_Rovnyagin/lab2_rovnyagin/.cursor/debug-0da112.log");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private DebugProbe() {
    }

    public static void log(String runId, String hypothesisId, String location, String message, Map<String, Object> data) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", SESSION_ID);
        payload.put("runId", runId);
        payload.put("hypothesisId", hypothesisId);
        payload.put("location", location);
        payload.put("message", message);
        payload.put("data", data);
        payload.put("timestamp", System.currentTimeMillis());

        try {
            String line = OBJECT_MAPPER.writeValueAsString(payload) + System.lineSeparator();
            Files.writeString(LOG_PATH, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (JsonProcessingException ignored) {
        } catch (Exception ignored) {
        }
    }
}
