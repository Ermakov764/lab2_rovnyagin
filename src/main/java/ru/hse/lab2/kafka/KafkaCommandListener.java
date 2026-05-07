package ru.hse.lab2.kafka;

import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Единственная точка входа consumer: читает топик {@code lab.kafka.topic} и отдаёт управление
 * {@link KafkaCommandDispatcher}.
 * <p>
 * <strong>LAB13:</strong> включён <strong>batch</strong>-режим Spring Kafka — метод принимает список
 * {@link ConsumerRecord}, полученных одним poll (до {@code max-poll-records} записей). Для каждой записи
 * вызывается {@code dispatcher.dispatch(value)}; семантика обработки одной команды не меняется.
 * <p>
 * {@code concurrency} подставляется из конфигурации ({@code lab.kafka.listener-concurrency}) — это число
 * потоков listener container внутри процесса (для ТЗ: сравнение 1 vs 2 при двух партициях топика).
 */
@Component
public class KafkaCommandListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaCommandListener.class);

    private final KafkaCommandDispatcher dispatcher;

    public KafkaCommandListener(KafkaCommandDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    /**
     * Обрабатывает одну пачку записей из poll: для каждой — парсинг JSON и dispatch.
     * Исключения из {@link KafkaCommandDispatcher} / хендлеров логируются, запись пропускается (остальные в пачке
     * продолжают обрабатываться).
     */
    @KafkaListener(
            id = "lab2KafkaCommandListener",
            topics = "${lab.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "${lab.kafka.listener-concurrency}",
            containerFactory = "kafkaBatchListenerContainerFactory"
    )
    public void listen(List<ConsumerRecord<String, String>> records) {
        for (ConsumerRecord<String, String> record : records) {
            log.info(
                    "Received Kafka command from topic={}, partition={}, offset={}",
                    record.topic(),
                    record.partition(),
                    record.offset()
            );
            try {
                dispatcher.dispatch(record.value());
                log.info(
                        "Processed Kafka command from topic={}, partition={}, offset={}",
                        record.topic(),
                        record.partition(),
                        record.offset()
                );
            } catch (RuntimeException e) {
                log.warn(
                        "Skipped Kafka command from topic={}, partition={}, offset={}: {}",
                        record.topic(),
                        record.partition(),
                        record.offset(),
                        e.getMessage()
                );
            }
        }
    }
}
