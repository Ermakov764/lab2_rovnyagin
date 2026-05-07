package ru.hse.lab2.kafka;

/**
 * Тип сущности в JSON-команде Kafka (поле {@code entity}).
 * <p>
 * Соответствует разделам API и отдельным {@link KafkaCommandHandler}.
 */
public enum EntityType {
    FILM,
    VIEWER,
    TICKET
}
