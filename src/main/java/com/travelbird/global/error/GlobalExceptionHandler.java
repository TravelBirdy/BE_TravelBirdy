package com.travelbird.global.error;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorCode code = ex.errorCode();

        return ResponseEntity
            .status(code.httpStatus())
            .body(ErrorResponse.of(code, ex.getMessage(), ex.details()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put(
            "fields",
            ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                    "field", error.getField(),
                    "message", error.getDefaultMessage() == null
                        ? "invalid value"
                        : error.getDefaultMessage()
                ))
                .toList()
        );

        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.of(
                ErrorCode.INVALID_REQUEST,
                ErrorCode.INVALID_REQUEST.defaultMessage(),
                details
            ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
        ConstraintViolationException ex
    ) {
        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.of(
                ErrorCode.INVALID_REQUEST,
                ex.getMessage(),
                null
            ));
    }

    /**
     * Expected database unique/FK conflicts must be translated to a domain-specific
     * BusinessException inside the Service layer whenever the violated constraint is known.
     * This generic handler is the fail-safe for an unmapped integrity violation.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
        DataIntegrityViolationException ex
    ) {
        return ResponseEntity
            .status(ErrorCode.INTERNAL_SERVER_ERROR.httpStatus())
            .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity
            .status(ErrorCode.INTERNAL_SERVER_ERROR.httpStatus())
            .body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
