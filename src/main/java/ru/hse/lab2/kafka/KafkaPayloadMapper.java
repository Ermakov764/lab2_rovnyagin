package ru.hse.lab2.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import ru.hse.lab2.exception.ValidationException;

/**
 * Преобразование {@code payload} из Kafka JSON в DTO запросов REST-сервисов.
 * <p>
 * Для PUT/DEL в Kafka часто передают {@code id} внутри payload вместе с полями сущности; для вызова
 * сервиса {@code id} извлекается через {@link #requireId(JsonNode)}, а в объект запроса для create/update
 * поле {@code id} убирается {@link #payloadWithoutId(JsonNode)}, чтобы не дублировать его в теле.
 */
@Component
public class KafkaPayloadMapper {

    private final ObjectMapper objectMapper;

    public KafkaPayloadMapper() {
        this.objectMapper = KafkaJson.objectMapper();
    }

    /**
     * Обязательное числовое поле {@code id} в JSON (для update/delete).
     */
    public Long requireId(JsonNode payload) {
        if (payload == null || !payload.hasNonNull("id") || !payload.get("id").canConvertToLong()) {
            throw new ValidationException("Kafka payload must contain numeric id");
        }
        return payload.get("id").asLong();
    }

    /**
     * Десериализация payload в тип запроса (например {@code FilmRequest}); перед этим удаляется {@code id}
     * из копии объекта, если payload — JSON-object.
     */
    public <T> T toRequest(JsonNode payload, Class<T> requestType) {
        if (payload == null || payload.isNull()) {
            throw new ValidationException("Kafka payload must not be null");
        }
        return objectMapper.convertValue(payloadWithoutId(payload), requestType);
    }

    private JsonNode payloadWithoutId(JsonNode payload) {
        if (!payload.isObject()) {
            return payload;
        }
        ObjectNode copy = payload.deepCopy();
        copy.remove("id");
        return copy;
    }
}
