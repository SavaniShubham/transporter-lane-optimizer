package com.freightfox.laneoptimizer.domain;

import java.util.List;

/**
 * Immutable result of the lane optimization computation.
 *
 * @param totalCost the sum of quote costs across all assigned lanes
 * @param assignments the list of lane-to-transporter assignments
 * @param selectedTransporterIds distinct IDs of transporters actually assigned at least one lane
 */
public record OptimizationResult(
        Long totalCost,
        List<LaneAssignment> assignments,
        List<Long> selectedTransporterIds
) {
    public OptimizationResult {
        if (totalCost == null || totalCost < 0) {
            throw new IllegalArgumentException("totalCost must be non-negative");
        }
        if (assignments == null) {
            throw new IllegalArgumentException("assignments cannot be null");
        }
        if (selectedTransporterIds == null) {
            throw new IllegalArgumentException("selectedTransporterIds cannot be null");
        }
        assignments = List.copyOf(assignments);
        selectedTransporterIds = List.copyOf(selectedTransporterIds);
    }
}
