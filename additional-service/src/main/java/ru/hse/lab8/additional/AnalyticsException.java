package ru.hse.lab8.additional;

import org.springframework.http.HttpStatus;

/**
 * Единая контролируемая ошибка аналитики: сообщение для тела ответа и HTTP-статус.
 */
public final class AnalyticsException extends RuntimeException {

    private final HttpStatus status;

    public AnalyticsException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
