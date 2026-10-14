package com.laneoptimizer.exception;

/**
 * Thrown when an assignment is requested before any valid input data has been submitted.
 */
public class NoInputDataException extends RuntimeException {

    public NoInputDataException(String message) {
        super(message);
    }

    public NoInputDataException() {
        super("No input data found. Please submit lane and transporter data via POST /api/v1/transporters/input first.");
    }
}
