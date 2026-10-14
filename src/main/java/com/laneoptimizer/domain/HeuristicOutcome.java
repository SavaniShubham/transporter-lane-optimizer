package com.laneoptimizer.domain;

/**
 * Sealed interface representing the outcome of a heuristic local search.
 */
public sealed interface HeuristicOutcome {

    /**
     * A valid feasible assignment was found (best-effort, not proven optimal).
     */
    record Feasible(OptimizationResult result) implements HeuristicOutcome {
        public Feasible {
            if (result == null) {
                throw new IllegalArgumentException("Feasible result cannot be null");
            }
        }
    }

    /**
     * The greedy seed phase failed to cover all lanes within the K transporter limit.
     */
    record SeedFailed() implements HeuristicOutcome {}
}
