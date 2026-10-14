package com.laneoptimizer.domain;

import java.util.List;

/**
 * Sealed interface representing the outcome of an exhaustive exact search.
 */
public sealed interface ExactOutcome {

    /**
     * All candidate subsets at the optimal transporter usage level were fully explored.
     * The result is proven optimal under Interpretation B.
     */
    record Optimal(OptimizationResult result) implements ExactOutcome {
        public Optimal {
            if (result == null) {
                throw new IllegalArgumentException("Optimal result cannot be null");
            }
        }
    }

    /**
     * Node budget was exhausted during the exact search.
     * {@code bestSoFar} contains the best valid result found before stopping, or {@code null} if none was found.
     */
    record Exhausted(OptimizationResult bestSoFar) implements ExactOutcome {}

    /**
     * All candidate subsets at all valid usage levels were fully explored, and no valid assignment exists.
     * The problem is proven infeasible.
     */
    record Infeasible(List<Long> uncoveredLaneIds) implements ExactOutcome {
        public Infeasible {
            uncoveredLaneIds = (uncoveredLaneIds == null) ? List.of() : List.copyOf(uncoveredLaneIds);
        }
    }
}
