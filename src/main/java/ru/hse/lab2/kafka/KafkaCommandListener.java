package ru.hse.lab2.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaCommandListener {

    private static final Logger log = LoggerFactory.getLogger(KafkaCommandListener.class);

    private final KafkaCommandDispatcher dispatcher;

    public KafkaCommandListener(KafkaCommandDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @KafkaListener(
            id = "lab2KafkaCommandListener",
            topics = "${lab.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            concurrency = "${lab.kafka.listener-concurrency}"
    )
    public void listen(ConsumerRecord<String, String> record) {
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
