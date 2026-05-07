package ru.hse.lab2.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import ru.hse.lab2.api.request.TicketRequest;
import ru.hse.lab2.exception.ValidationException;
import ru.hse.lab2.service.TicketService;

/**
 * Команды Kafka с {@link EntityType#TICKET} → {@link TicketService}.
 */
@Component
public class TicketKafkaCommandHandler implements KafkaCommandHandler {

    private final TicketService ticketService;
    private final KafkaPayloadMapper payloadMapper;

    public TicketKafkaCommandHandler(TicketService ticketService, KafkaPayloadMapper payloadMapper) {
        this.ticketService = ticketService;
        this.payloadMapper = payloadMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.TICKET;
    }

    @Override
    public void handle(OperationType operation, JsonNode payload) {
        switch (operation) {
            case POST -> ticketService.create(payloadMapper.toRequest(payload, TicketRequest.class));
            case PUT -> ticketService.update(
                    payloadMapper.requireId(payload),
                    payloadMapper.toRequest(payload, TicketRequest.class)
            );
            case DEL -> ticketService.delete(payloadMapper.requireId(payload));
            default -> throw new ValidationException("Unsupported ticket operation: " + operation);
        }
    }
}
