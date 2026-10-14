package com.freightfox.laneoptimizer.domain;

/**
 * Immutable representation of a lane assignment to a transporter with associated cost.
 *
 * @param laneId the unique identifier of the assigned lane
 * @param transporterId the unique identifier of the assigned transporter
 * @param cost the quote cost for this assignment
 */
public record LaneAssignment(Long laneId, Long transporterId, Long cost) {
    public LaneAssignment {
        if (laneId == null) {
            throw new IllegalArgumentException("laneId cannot be null");
        }
        if (transporterId == null) {
            throw new IllegalArgumentException("transporterId cannot be null");
        }
        if (cost == null || cost < 0) {
            throw new IllegalArgumentException("cost must be non-negative");
        }
    }
}
