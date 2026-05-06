package ru.hse.lab2.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import ru.hse.lab2.api.request.ViewerRequest;
import ru.hse.lab2.exception.ValidationException;
import ru.hse.lab2.service.ViewerService;

@Component
public class ViewerKafkaCommandHandler implements KafkaCommandHandler {

    private final ViewerService viewerService;
    private final KafkaPayloadMapper payloadMapper;

    public ViewerKafkaCommandHandler(ViewerService viewerService, KafkaPayloadMapper payloadMapper) {
        this.viewerService = viewerService;
        this.payloadMapper = payloadMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.VIEWER;
    }

    @Override
    public void handle(OperationType operation, JsonNode payload) {
        switch (operation) {
            case POST -> viewerService.create(payloadMapper.toRequest(payload, ViewerRequest.class));
            case PUT -> viewerService.update(
                    payloadMapper.requireId(payload),
                    payloadMapper.toRequest(payload, ViewerRequest.class)
            );
            case DEL -> viewerService.delete(payloadMapper.requireId(payload));
            default -> throw new ValidationException("Unsupported viewer operation: " + operation);
        }
    }
}
