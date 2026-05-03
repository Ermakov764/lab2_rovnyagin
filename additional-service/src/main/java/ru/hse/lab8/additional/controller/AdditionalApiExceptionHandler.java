package ru.hse.lab8.additional.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import ru.hse.lab8.additional.AnalyticsException;

import java.net.URI;
import java.util.stream.Collectors;

/**
 * Ошибки в формате RFC 7807 {@link ProblemDetail} (JSON: {@code type}, {@code title}, {@code status}, {@code detail}).
 */
@RestControllerAdvice
public class AdditionalApiExceptionHandler {

    private static final URI ABOUT_BLANK = URI.create("about:blank");

    @ExceptionHandler(AnalyticsException.class)
    public ResponseEntity<ProblemDetail> handleAnalytics(AnalyticsException e) {
        HttpStatus status = e.getStatus();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        pd.setType(ABOUT_BLANK);
        pd.setTitle(status.getReasonPhrase());
        return ResponseEntity.status(status).body(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException e) {
        String detail = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setType(ABOUT_BLANK);
        pd.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        return ResponseEntity.badRequest().body(pd);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        String detail = e.getParameterValidationResults().stream()
                .flatMap(p -> p.getResolvableErrors().stream())
                .map(AdditionalApiExceptionHandler::messageOf)
                .collect(Collectors.joining("; "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setType(ABOUT_BLANK);
        pd.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        return ResponseEntity.badRequest().body(pd);
    }

    private static String messageOf(MessageSourceResolvable r) {
        String m = r.getDefaultMessage();
        if (m != null) {
            return m;
        }
        String[] codes = r.getCodes();
        return (codes != null && codes.length > 0) ? codes[0] : r.toString();
    }
}
