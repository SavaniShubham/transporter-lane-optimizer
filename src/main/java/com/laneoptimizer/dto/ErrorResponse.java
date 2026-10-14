package com.laneoptimizer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Standard error response envelope.
 */
public record ErrorResponse(
        @Schema(description = "Error status indicator", example = "error")
        String status,

        @Schema(description = "High-level error summary message", example = "Validation failed")
        String message,

        @Schema(description = "Detailed error messages or violated constraints")
        List<String> details
) {
    public static ErrorResponse of(String message, List<String> details) {
        return new ErrorResponse("error", message, details != null ? List.copyOf(details) : List.of());
    }

    public static ErrorResponse of(String message, String detail) {
        return of(message, List.of(detail));
    }
}
