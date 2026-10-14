package com.laneoptimizer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload specifying the maximum transporter usage constraint.
 */
public record AssignmentRequest(
        @Schema(description = "Maximum allowed number of distinct transporters actually assigned lanes (K >= 1)", example = "3", minimum = "1")
        @NotNull(message = "maxTransporters cannot be null")
        @Min(value = 1, message = "maxTransporters must be at least 1")
        Integer maxTransporters
) {}
