package com.freightfox.laneoptimizer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Data transfer object representing a trade lane input.
 */
public record LaneDto(
        @Schema(description = "Unique identifier of the lane", example = "1")
        @NotNull(message = "Lane id cannot be null")
        Long id,

        @Schema(description = "Origin location of the lane", example = "Mumbai")
        @NotBlank(message = "Lane origin cannot be blank")
        String origin,

        @Schema(description = "Destination location of the lane", example = "Delhi")
        @NotBlank(message = "Lane destination cannot be blank")
        String destination
) {}
