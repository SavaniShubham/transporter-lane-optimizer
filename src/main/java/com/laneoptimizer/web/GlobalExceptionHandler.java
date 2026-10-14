package com.laneoptimizer.web;

import com.laneoptimizer.dto.ErrorResponse;
import com.laneoptimizer.exception.InfeasibleAssignmentException;
import com.laneoptimizer.exception.InvalidInputException;
import com.laneoptimizer.exception.NoInputDataException;
import com.laneoptimizer.exception.SolverBudgetExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized exception handler mapping domain and system exceptions to standardized HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> details = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            details.add(error.getField() + ": " + error.getDefaultMessage());
        }
        ErrorResponse body = ErrorResponse.of("Validation failed for request body", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        ErrorResponse body = ErrorResponse.of(
                "Malformed JSON request body",
                ex.getMessage() != null ? ex.getMessage() : "Unable to parse request body"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidInputException ex) {
        ErrorResponse body = ErrorResponse.of(ex.getMessage(), ex.getDetails());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(NoInputDataException.class)
    public ResponseEntity<ErrorResponse> handleNoInputData(NoInputDataException ex) {
        ErrorResponse body = ErrorResponse.of(ex.getMessage(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(InfeasibleAssignmentException.class)
    public ResponseEntity<ErrorResponse> handleInfeasible(InfeasibleAssignmentException ex) {
        List<String> details = new ArrayList<>();
        if (!ex.getUncoveredLaneIds().isEmpty()) {
            details.add("Uncovered lane IDs: " + ex.getUncoveredLaneIds());
        } else {
            details.add(ex.getMessage());
        }
        ErrorResponse body = ErrorResponse.of(ex.getMessage(), details);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(body);
    }

    @ExceptionHandler(SolverBudgetExceededException.class)
    public ResponseEntity<ErrorResponse> handleBudgetExceeded(SolverBudgetExceededException ex) {
        ErrorResponse body = ErrorResponse.of(ex.getMessage(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled server exception encountered: ", ex);
        ErrorResponse body = ErrorResponse.of(
                "An unexpected internal server error occurred",
                "Internal server error. Please verify the request or contact support if the issue persists."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
