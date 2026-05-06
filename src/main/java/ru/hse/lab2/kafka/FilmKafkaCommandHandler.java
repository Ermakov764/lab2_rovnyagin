package ru.hse.lab2.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import ru.hse.lab2.api.request.FilmRequest;
import ru.hse.lab2.exception.ValidationException;
import ru.hse.lab2.service.FilmService;

@Component
public class FilmKafkaCommandHandler implements KafkaCommandHandler {

    private final FilmService filmService;
    private final KafkaPayloadMapper payloadMapper;

    public FilmKafkaCommandHandler(FilmService filmService, KafkaPayloadMapper payloadMapper) {
        this.filmService = filmService;
        this.payloadMapper = payloadMapper;
    }

    @Override
    public EntityType entityType() {
        return EntityType.FILM;
    }

    @Override
    public void handle(OperationType operation, JsonNode payload) {
        switch (operation) {
            case POST -> filmService.create(payloadMapper.toRequest(payload, FilmRequest.class));
            case PUT -> filmService.update(
                    payloadMapper.requireId(payload),
                    payloadMapper.toRequest(payload, FilmRequest.class)
            );
            case DEL -> filmService.delete(payloadMapper.requireId(payload));
            default -> throw new ValidationException("Unsupported film operation: " + operation);
        }
    }
}
