package com.laneoptimizer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Single lane-to-transporter assignment within the optimization response.
 */
public record AssignmentItem(
        @Schema(description = "Assigned trade lane ID", example = "1")
        Long laneId,

        @Schema(description = "Assigned transporter ID", example = "5")
        Long transporterId,

        @Schema(description = "Assigned quote cost for this lane", example = "11601")
        Long cost
) {}
