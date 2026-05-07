package ru.hse.lab2.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import ru.hse.lab2.exception.ValidationException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Разбор строкового JSON из Kafka и маршрутизация к нужному {@link KafkaCommandHandler}.
 * <p>
 * Конструктор собирает карту {@link EntityType} → хендлер из всех бинов {@link KafkaCommandHandler}
 * (FILM / VIEWER / TICKET). Цикл listener → dispatcher → handler повторяется для каждого сообщения,
 * в том числе внутри batch-вызова {@link KafkaCommandListener#listen}.
 */
@Component
public class KafkaCommandDispatcher {

    private final ObjectMapper objectMapper;
    private final Map<EntityType, KafkaCommandHandler> handlers;

    public KafkaCommandDispatcher(List<KafkaCommandHandler> handlers) {
        this.objectMapper = KafkaJson.objectMapper();
        this.handlers = new EnumMap<>(EntityType.class);
        handlers.forEach(handler -> this.handlers.put(handler.entityType(), handler));
    }

    /**
     * Распарсить JSON, проверить обязательные поля, делегировать сущностному хендлеру.
     * Ошибки валидации/JSON — {@link ValidationException}; остальные из хендлеров пробрасываются в listener.
     */
    public void dispatch(String rawMessage) {
        KafkaCommandMessage command = parse(rawMessage);
        validate(command);

        KafkaCommandHandler handler = handlers.get(command.getEntity());
        if (handler == null) {
            throw new ValidationException("Unsupported Kafka entity: " + command.getEntity());
        }
        handler.handle(command.getOperation(), command.getPayload());
    }

    private KafkaCommandMessage parse(String rawMessage) {
        try {
            return objectMapper.readValue(rawMessage, KafkaCommandMessage.class);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Kafka message must be valid JSON");
        }
    }

    private void validate(KafkaCommandMessage command) {
        if (command.getEntity() == null) {
            throw new ValidationException("Kafka message entity must not be null");
        }
        if (command.getOperation() == null) {
            throw new ValidationException("Kafka message operation must not be null");
        }
        if (command.getPayload() == null || command.getPayload().isNull()) {
            throw new ValidationException("Kafka message payload must not be null");
        }
    }
}
