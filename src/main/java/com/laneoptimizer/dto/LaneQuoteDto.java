package com.laneoptimizer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Data transfer object representing a transporter quote for a specific lane.
 */
public record LaneQuoteDto(
        @Schema(description = "Identifier of the lane being quoted", example = "1")
        @NotNull(message = "Lane id in quote cannot be null")
        Long laneId,

        @Schema(description = "Quote amount in integer currency units", example = "5000")
        @NotNull(message = "Quote amount cannot be null")
        @Positive(message = "Quote amount must be positive (> 0)")
        Long quote
) {}
