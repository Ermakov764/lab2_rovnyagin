package ru.hse.lab2.kafka;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Корневой JSON одной команды из топика (как при отправке через {@code send_kafka_message.py} или прокси LAB13).
 * <p>
 * Поля соответствуют контракту курса: какую сущность трогать, какую операцию, и произвольное тело в
 * {@code payload} (часто вложенный объект с полями сущности и при необходимости {@code id}).
 */
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
