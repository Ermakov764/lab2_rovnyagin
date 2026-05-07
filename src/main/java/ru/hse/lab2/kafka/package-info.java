/**
 * Слой интеграции с <strong>Kafka</strong> для курса: чтение JSON-команд из топика варианта и применение
 * к тем же сервисам, что и REST.
 *
 * <h2>Лаб. 12 (основа)</h2>
 * <ul>
 *   <li>Сообщение в топике — JSON с полями {@code entity}, {@code operation}, {@code payload} (см. {@link
 *   KafkaCommandMessage}).</li>
 *   <li>{@link KafkaCommandListener} (раньше — по одной записи) передаёт тело в {@link KafkaCommandDispatcher}.</li>
 *   <li>Диспетчер выбирает {@link KafkaCommandHandler} по {@link EntityType} и вызывает {@code handle}.</li>
 * </ul>
 *
 * <h2>Лаб. 13 (нагрузка, batch, concurrency)</h2>
 * <ul>
 *   <li>Листенер переведён на <strong>batch</strong>: Spring отдаёт {@link java.util.List} из {@link
 *   org.apache.kafka.clients.consumer.ConsumerRecord}; внутри цикла по записям логика та же, что и для
 *   одиночного сообщения.</li>
 *   <li>Фабрика {@code kafkaBatchListenerContainerFactory} в {@link KafkaBatchListenerConfig} с
 *   {@code setBatchListener(true)}; размер пачки из poll задаётся {@code spring.kafka.consumer.max-poll-records}.</li>
 *   <li>Число потоков листенера (сравнение в ТЗ) — {@code lab.kafka.listener-concurrency} / {@code
 *   KAFKA_LISTENER_CONCURRENCY} в {@code application.properties}.</li>
 *   <li>Запись в топик с нагрузочной ВМ — не через {@code POST /api/viewers}, а через внешний REST-прокси
 *   (см. {@code k6/kafka-proxy/}) в том же формате, что и {@code scripts/send_kafka_message.py}.</li>
 * </ul>
 *
 * <p>Число <strong>партиций</strong> топика настраивается на кластере Kafka, не в этом пакете.</p>
 */
package ru.hse.lab2.kafka;
