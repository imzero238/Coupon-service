package com.ecommerce.couponservice.exception;

import com.ecommerce.couponservice.exception.response.ExceptionResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InternalEventException.class)
    public ResponseEntity<?> handleInternalEventException(InternalEventException e) {
        return handleExceptionInternal(e.getExceptionCode());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .code(String.valueOf(ExceptionCode.NOT_VALID))
                .message(Objects.requireNonNull(ex.getBindingResult().getFieldError()).getDefaultMessage())
                .build();

        return new ResponseEntity<>(exceptionResponse, ExceptionCode.NOT_VALID.getHttpStatus());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException e) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .code(String.valueOf(ExceptionCode.ILLEGAL_ARGUMENT))
                .message(e.getMessage())
                .build();

        return new ResponseEntity<>(exceptionResponse, ExceptionCode.ILLEGAL_ARGUMENT.getHttpStatus());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<?> handleConstraintViolationException(ConstraintViolationException e) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .code(String.valueOf(ExceptionCode.CONSTRAINT_VIOLATION))
                .message(e.getMessage())
                .build();

        return new ResponseEntity<>(exceptionResponse, ExceptionCode.CONSTRAINT_VIOLATION.getHttpStatus());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleEntityNotFoundException(EntityNotFoundException e) {
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .code(String.valueOf(ExceptionCode.NOT_FOUND))
                .message(e.getMessage())
                .build();

        return new ResponseEntity<>(exceptionResponse, ExceptionCode.NOT_FOUND.getHttpStatus());
    }

    private ResponseEntity<Object> handleExceptionInternal(ExceptionCode exceptionCode) {
        return ResponseEntity.status(exceptionCode.getHttpStatus())
                .body(createExceptionResponse(exceptionCode));
    }

    private ExceptionResponse createExceptionResponse(ExceptionCode exceptionCode) {
        return ExceptionResponse.builder()
                .code(exceptionCode.name())
                .message(exceptionCode.getMessage())
                .build();
    }
}
