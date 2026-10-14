package com.freightfox.laneoptimizer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request payload containing all trade lanes and transporters with quotes.
 */
public record InputRequest(
        @Schema(description = "List of trade lanes to be covered")
        @NotEmpty(message = "Lanes list cannot be empty")
        @Valid
        List<LaneDto> lanes,

        @Schema(description = "List of transporters submitting quotes")
        @NotEmpty(message = "Transporters list cannot be empty")
        @Valid
        List<TransporterDto> transporters
) {}
