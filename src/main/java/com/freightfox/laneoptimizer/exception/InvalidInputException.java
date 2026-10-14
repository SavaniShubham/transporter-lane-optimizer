package com.freightfox.laneoptimizer.exception;

import java.util.List;

/**
 * Thrown when submitted input data fails business validation rules.
 */
public class InvalidInputException extends RuntimeException {

    private final List<String> details;

    public InvalidInputException(String message, List<String> details) {
        super(message);
        this.details = details != null ? List.copyOf(details) : List.of();
    }

    public InvalidInputException(String message) {
        this(message, List.of(message));
    }

    public List<String> getDetails() {
        return details;
    }
}
