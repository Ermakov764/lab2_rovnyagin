package ru.hse.lab2.kafka;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

/**
 * Конфигурация фабрики listener container для <strong>batch</strong>-режима (LAB13).
 * <p>
 * Spring Boot по умолчанию создаёт не-batch фабрику; здесь отдельный бин
 * {@code kafkaBatchListenerContainerFactory}, на который ссылается {@link KafkaCommandListener} через
 * {@code containerFactory}. Так не ломаются другие возможные Kafka-listener’ы с дефолтной фабрикой.
 * <p>
 * {@link ConcurrentKafkaListenerContainerFactory#setBatchListener(boolean)} = {@code true} означает:
 * метод listener получает список записей за один вызов, а не по одной записи.
 */
@Configuration
public class KafkaBatchListenerConfig {

    /**
     * Фабрика для {@link KafkaCommandListener}: consumer из Spring Boot + {@code setBatchListener(true)}.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaBatchListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        return factory;
    }
}
