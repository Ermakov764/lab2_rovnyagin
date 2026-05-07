package ru.hse.lab2.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * Общий {@link ObjectMapper} для JSON из Kafka: Java time-модули, неизвестные поля в DTO не падают
 * (удобно при эволюции схемы сообщений).
 */
final class KafkaJson {

    private KafkaJson() {
    }

    static ObjectMapper objectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
    }
}
