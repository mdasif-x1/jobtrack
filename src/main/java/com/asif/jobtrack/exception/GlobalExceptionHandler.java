package com.asif.jobtrack.exception;

import com.asif.jobtrack.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request,
                new LinkedHashMap<>()
        );
    }

    @ExceptionHandler({
            DuplicateResourceException.class,
            CompanyInUseException.class
    })
    public ResponseEntity<ErrorResponse> handleConflictException(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request,
                new LinkedHashMap<>()
        );
    }

    @ExceptionHandler({
            InvalidApplicationException.class,
            InvalidRequestException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request,
                new LinkedHashMap<>()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError :
                exception.getBindingResult().getFieldErrors()) {

            fieldErrors.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String message = "Invalid value for parameter: "
                + exception.getName();

        Class<?> requiredType = exception.getRequiredType();

        if (requiredType != null && requiredType.isEnum()) {
            message += ". Allowed values: "
                    + Arrays.toString(requiredType.getEnumConstants());
        }

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request,
                new LinkedHashMap<>()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJsonException(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {


        InvalidFormatException invalidFormatException =
                findInvalidFormatException(exception);

        if (invalidFormatException != null) {
            Class<?> targetType =
                    invalidFormatException.getTargetType();

            if (targetType != null && targetType.isEnum()) {
                String message = "Invalid enum value '"
                        + invalidFormatException.getValue()
                        + "'. Allowed values: "
                        + Arrays.toString(targetType.getEnumConstants());

                return buildErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        message,
                        request,
                        new LinkedHashMap<>()
                );
            }
        }

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed JSON request",
                request,
                new LinkedHashMap<>()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        logger.error(
                "Unexpected error for request: {}",
                request.getRequestURI(),
                exception
        );

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request,
                new LinkedHashMap<>()
        );
    }

    private InvalidFormatException findInvalidFormatException(
            Throwable exception
    ) {
        Throwable currentException = exception;

        while (currentException != null) {
            if (currentException instanceof InvalidFormatException) {
                return (InvalidFormatException) currentException;
            }

            currentException = currentException.getCause();
        }

        return null;
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors
    ) {
        ErrorResponse errorResponse = new ErrorResponse();

        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setStatus(status.value());
        errorResponse.setError(status.getReasonPhrase());
        errorResponse.setMessage(message);
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setFieldErrors(fieldErrors);

        return ResponseEntity
                .status(status)
                .body(errorResponse);
    }
}