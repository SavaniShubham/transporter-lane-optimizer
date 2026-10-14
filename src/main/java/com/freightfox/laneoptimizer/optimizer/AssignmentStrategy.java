package com.freightfox.laneoptimizer.optimizer;

/**
 * Common interface for lane assignment strategies.
 */
public interface AssignmentStrategy {

    /**
     * @return human-readable name of the strategy
     */
    String name();
}
