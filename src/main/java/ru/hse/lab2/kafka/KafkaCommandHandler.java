package ru.hse.lab2.kafka;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Обработчик команд Kafka для одной сущности ({@link EntityType}).
 * <p>
 * Реализации — отдельные Spring-бины ({@code Film*}, {@code Viewer*}, {@code Ticket*}); регистрируются в
 * {@link KafkaCommandDispatcher} по {@link #entityType()}.
 */
public interface KafkaCommandHandler {

    /** Сущность, для которой этот хендлер отвечает (ключ в диспетчере). */
    EntityType entityType();

    /**
     * Выполнить операцию с телом из Kafka. Контракт payload совместим с тем, что ожидают REST-сервисы
     * после маппинга {@link KafkaPayloadMapper}.
     */
    void handle(OperationType operation, JsonNode payload);
}
