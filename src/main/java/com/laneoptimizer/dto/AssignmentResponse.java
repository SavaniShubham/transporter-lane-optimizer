package com.laneoptimizer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Successful response payload for the lane optimization assignment endpoint.
 */
public record AssignmentResponse(
        @Schema(description = "Response status", example = "success")
        String status,

        @Schema(description = "Total cost across all assigned lanes", example = "60139")
        Long totalCost,

        @Schema(description = "True if proven mathematically optimal under the lexicographic objective; false if determined via heuristic search", example = "true")
        Boolean optimal,

        @Schema(description = "List of lane assignments")
        List<AssignmentItem> assignments,

        @Schema(description = "List of distinct transporter IDs actually assigned at least one lane", example = "[1, 4, 5]")
        List<Long> selectedTransporters
) {}
