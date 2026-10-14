package com.freightfox.laneoptimizer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard status response for data ingestion.
 */
public record StatusMessageResponse(
        @Schema(description = "Response status", example = "success")
        String status,

        @Schema(description = "Human-readable confirmation message", example = "Input data saved successfully.")
        String message
) {
    public static StatusMessageResponse success(String message) {
        return new StatusMessageResponse("success", message);
    }
}
