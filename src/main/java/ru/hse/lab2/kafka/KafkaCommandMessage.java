package ru.hse.lab2.kafka;

import com.fasterxml.jackson.databind.JsonNode;

public class KafkaCommandMessage {

    private EntityType entity;
    private OperationType operation;
    private JsonNode payload;

    public EntityType getEntity() {
        return entity;
    }

    public void setEntity(EntityType entity) {
        this.entity = entity;
    }

    public OperationType getOperation() {
        return operation;
    }

    public void setOperation(OperationType operation) {
        this.operation = operation;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public void setPayload(JsonNode payload) {
        this.payload = payload;
    }
}
