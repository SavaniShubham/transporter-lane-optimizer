package com.laneoptimizer.optimizer;

import com.laneoptimizer.domain.ExactOutcome;
import com.laneoptimizer.domain.HeuristicOutcome;
import com.laneoptimizer.domain.OptimizationResult;
import com.laneoptimizer.exception.InfeasibleAssignmentException;
import com.laneoptimizer.exception.SolverBudgetExceededException;

import java.util.List;

/**
 * Facade coordinating exact and heuristic optimization strategies.
 * Pure Java: no Spring dependencies.
 */
public class LaneOptimizer {

    private final ExhaustiveStrategy exhaustiveStrategy;
    private final LocalSearchStrategy localSearchStrategy;

    public LaneOptimizer() {
        this(new ExhaustiveStrategy(), new LocalSearchStrategy());
    }

    public LaneOptimizer(ExhaustiveStrategy exhaustiveStrategy, LocalSearchStrategy localSearchStrategy) {
        this.exhaustiveStrategy = exhaustiveStrategy;
        this.localSearchStrategy = localSearchStrategy;
    }

    /**
     * Optimization output containing the domain result and an optimality guarantee flag.
     *
     * @param result the underlying optimization result
     * @param optimal true if proven mathematically optimal under Interpretation B; false if heuristic or budget-bounded
     */
    public record FinalResult(OptimizationResult result, boolean optimal) {}

    /**
     * Solves lane assignment adhering to the 5-step fallback policy.
     *
     * @param matrix the dense quote matrix
     * @param maxTransporters user-specified ceiling K
     * @return FinalResult with assignment details and optimal flag
     */
    public FinalResult optimize(QuoteMatrix matrix, int maxTransporters) {
        if (matrix == null) {
            throw new IllegalArgumentException("QuoteMatrix cannot be null");
        }
        if (maxTransporters < 1) {
            throw new IllegalArgumentException("maxTransporters must be at least 1");
        }

        // 1. Pre-check: orphan lanes
        List<Long> orphanLanes = matrix.lanesWithoutAnyQuote();
        if (!orphanLanes.isEmpty()) {
            throw new InfeasibleAssignmentException(
                    "Problem is infeasible: Trade lanes have no quotes from any transporter: " + orphanLanes,
                    orphanLanes
            );
        }

        int numTransporters = matrix.numTransporters();
        int numLanes = matrix.numLanes();
        long subsets = totalSubsets(numTransporters, maxTransporters, numLanes);

        // 2 & 3. If within subset threshold, run exhaustive search
        if (subsets <= ExhaustiveStrategy.SUBSET_THRESHOLD) {
            ExactOutcome outcome = exhaustiveStrategy.solve(matrix, maxTransporters);
            if (outcome instanceof ExactOutcome.Optimal(OptimizationResult res)) {
                return new FinalResult(res, true);
            } else if (outcome instanceof ExactOutcome.Infeasible(List<Long> uncovered)) {
                throw new InfeasibleAssignmentException(
                        "Problem is infeasible: No valid assignment can cover all lanes with at most "
                                + maxTransporters + " transporters.",
                        uncovered
                );
            } else if (outcome instanceof ExactOutcome.Exhausted(OptimizationResult bestSoFar)) {
                if (bestSoFar != null) {
                    return new FinalResult(bestSoFar, false);
                }
                // Fall through to step 4 if Exhausted(null)
            }
        }

        // 4. Run heuristic local search
        HeuristicOutcome heuristicOutcome = localSearchStrategy.solve(matrix, maxTransporters);
        if (heuristicOutcome instanceof HeuristicOutcome.Feasible(OptimizationResult res)) {
            return new FinalResult(res, false);
        }

        // 5. Last resort: bounded exhaustive search
        ExactOutcome lastResortOutcome = exhaustiveStrategy.solve(matrix, maxTransporters);
        if (lastResortOutcome instanceof ExactOutcome.Optimal(OptimizationResult res)) {
            return new FinalResult(res, true);
        } else if (lastResortOutcome instanceof ExactOutcome.Infeasible(List<Long> uncovered)) {
            throw new InfeasibleAssignmentException(
                    "Problem is infeasible: No valid assignment can cover all lanes with at most "
                            + maxTransporters + " transporters.",
                    uncovered
            );
        } else if (lastResortOutcome instanceof ExactOutcome.Exhausted(OptimizationResult bestSoFar)) {
            if (bestSoFar != null) {
                return new FinalResult(bestSoFar, false);
            }
            // Exhausted(null): budget exhausted and heuristic failed. Feasibility undetermined. Never 422!
            throw new SolverBudgetExceededException(
                    "Feasibility could not be determined within the search budget. "
                            + "Try reducing the number of transporters or lanes."
            );
        }

        throw new SolverBudgetExceededException("Unable to determine solution within search budget.");
    }

    /**
     * Calculates binomial coefficient C(n, k).
     * Protects against combinatorial integer overflow by capping at Long.MAX_VALUE.
     */
    public static long combinations(int n, int k) {
        if (k < 0 || k > n) {
            return 0L;
        }
        if (k == 0 || k == n) {
            return 1L;
        }
        if (k > n / 2) {
            k = n - k;
        }
        long res = 1L;
        for (int i = 1; i <= k; i++) {
            try {
                long next = Math.multiplyExact(res, (long) (n - i + 1));
                res = next / i;
            } catch (ArithmeticException e) {
                return Long.MAX_VALUE;
            }
        }
        return res;
    }

    /**
     * Sum of C(n, u) for u from 1 to min(k, n, l).
     * Protects against sum overflow by capping at Long.MAX_VALUE.
     */
    public static long totalSubsets(int n, int k, int l) {
        int maxU = Math.min(k, Math.min(n, l));
        long sum = 0L;
        for (int u = 1; u <= maxU; u++) {
            long c = combinations(n, u);
            if (c == Long.MAX_VALUE || Long.MAX_VALUE - sum < c) {
                return Long.MAX_VALUE;
            }
            sum += c;
        }
        return sum;
    }
}
