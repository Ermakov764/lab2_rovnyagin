package ru.hse.lab2.kafka;

import com.fasterxml.jackson.databind.JsonNode;

public interface KafkaCommandHandler {

    EntityType entityType();

    void handle(OperationType operation, JsonNode payload);
}
