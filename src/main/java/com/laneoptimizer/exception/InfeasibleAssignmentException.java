package com.laneoptimizer.exception;

import java.util.List;

/**
 * Thrown when an assignment is mathematically or structurally proven to be infeasible.
 */
public class InfeasibleAssignmentException extends RuntimeException {

    private final List<Long> uncoveredLaneIds;

    public InfeasibleAssignmentException(String message, List<Long> uncoveredLaneIds) {
        super(message);
        this.uncoveredLaneIds = uncoveredLaneIds != null ? List.copyOf(uncoveredLaneIds) : List.of();
    }

    public InfeasibleAssignmentException(String message) {
        this(message, List.of());
    }

    public List<Long> getUncoveredLaneIds() {
        return uncoveredLaneIds;
    }
}
