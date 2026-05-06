package ru.hse.lab2.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import ru.hse.lab2.exception.ValidationException;

@Component
public class KafkaPayloadMapper {

    private final ObjectMapper objectMapper;

    public KafkaPayloadMapper() {
        this.objectMapper = KafkaJson.objectMapper();
    }

    public Long requireId(JsonNode payload) {
        if (payload == null || !payload.hasNonNull("id") || !payload.get("id").canConvertToLong()) {
            throw new ValidationException("Kafka payload must contain numeric id");
        }
        return payload.get("id").asLong();
    }

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
