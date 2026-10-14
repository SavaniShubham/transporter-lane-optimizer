package com.laneoptimizer.optimizer;

import com.laneoptimizer.domain.ExactOutcome;
import com.laneoptimizer.domain.OptimizationResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Exact exhaustive assignment strategy implementing Interpretation B.
 * Maximizes distinct transporter usage up to min(K, N, L), then minimizes total cost.
 * Incorporates pruning rules (cost bound, pigeonhole, lower bound) and a global node budget.
 * Pure Java: no Spring dependencies.
 */
public class ExhaustiveStrategy implements AssignmentStrategy {

    public static final long SUBSET_THRESHOLD = 50_000L;
    public static final long NODE_BUDGET = 10_000_000L;

    private final long nodeBudget;
    private long nodesExplored;
    private boolean budgetExhausted;

    public ExhaustiveStrategy() {
        this(NODE_BUDGET);
    }

    public ExhaustiveStrategy(long nodeBudget) {
        this.nodeBudget = nodeBudget;
    }

    @Override
    public String name() {
        return "ExhaustiveStrategy";
    }

    public long getNodesExplored() {
        return nodesExplored;
    }

    /**
     * Solves the assignment problem exhaustively.
     *
     * @param matrix dense quote matrix
     * @param maxTransporters maximum allowed distinct transporters (K)
     * @return ExactOutcome (Optimal, Exhausted, or Infeasible)
     */
    public ExactOutcome solve(QuoteMatrix matrix, int maxTransporters) {
        this.nodesExplored = 0L;
        this.budgetExhausted = false;

        int numTransporters = matrix.numTransporters();
        int numLanes = matrix.numLanes();

        if (numLanes == 0 || numTransporters == 0 || maxTransporters < 1) {
            return new ExactOutcome.Infeasible(matrix.lanesWithoutAnyQuote());
        }

        // Early check for orphan lanes
        List<Long> orphanLanes = matrix.lanesWithoutAnyQuote();
        if (!orphanLanes.isEmpty()) {
            return new ExactOutcome.Infeasible(orphanLanes);
        }

        int maxPossibleUsage = Math.min(maxTransporters, Math.min(numTransporters, numLanes));

        OptimizationResult bestSoFar = null;

        // Iterate targetUsage from maxPossibleUsage down to 1
        for (int targetUsage = maxPossibleUsage; targetUsage >= 1; targetUsage--) {
            List<int[]> subsets = generateCombinations(numTransporters, targetUsage);

            Long bestCostAtThisUsage = null;
            int[] bestAssignmentAtThisUsage = null;

            for (int[] subset : subsets) {
                if (nodesExplored >= nodeBudget) {
                    budgetExhausted = true;
                    break;
                }

                // Check if this subset can cover every lane in principle
                if (!canSubsetCoverAllLanes(subset, matrix)) {
                    continue;
                }

                SubsetBounds result = solveConstrainedAssignment(subset, matrix, bestCostAtThisUsage);

                if (result.foundBetter()) {
                    bestCostAtThisUsage = result.bestCost();
                    bestAssignmentAtThisUsage = result.bestAssignment();
                }

                if (budgetExhausted) {
                    break;
                }
            }

            if (bestAssignmentAtThisUsage != null) {
                OptimizationResult optResult = ResultAssembler.assemble(bestAssignmentAtThisUsage, matrix);
                if (!budgetExhausted) {
                    // Highest targetUsage with a complete search found an optimal solution!
                    return new ExactOutcome.Optimal(optResult);
                } else {
                    return new ExactOutcome.Exhausted(optResult);
                }
            }

            if (budgetExhausted) {
                break;
            }
        }

        if (budgetExhausted) {
            return new ExactOutcome.Exhausted(bestSoFar);
        }

        return new ExactOutcome.Infeasible(List.of());
    }

    private boolean canSubsetCoverAllLanes(int[] subset, QuoteMatrix matrix) {
        int numLanes = matrix.numLanes();
        for (int l = 0; l < numLanes; l++) {
            boolean covered = false;
            for (int t : subset) {
                if (matrix.hasQuote(t, l)) {
                    covered = true;
                    break;
                }
            }
            if (!covered) {
                return false;
            }
        }
        return true;
    }

    private record SubsetBounds(boolean foundBetter, Long bestCost, int[] bestAssignment) {}

    private SubsetBounds solveConstrainedAssignment(int[] subset, QuoteMatrix matrix, Long upperBound) {
        int numLanes = matrix.numLanes();
        int subsetSize = subset.length;
        int targetMask = (1 << subsetSize) - 1;

        // Precompute lower bounds per lane for this subset
        long[] minQuotePerLane = new long[numLanes];
        long[] suffixMinSum = new long[numLanes + 1];

        for (int l = 0; l < numLanes; l++) {
            long minQ = Long.MAX_VALUE;
            for (int t : subset) {
                if (matrix.hasQuote(t, l)) {
                    long q = matrix.getQuote(t, l);
                    if (q < minQ) {
                        minQ = q;
                    }
                }
            }
            if (minQ == Long.MAX_VALUE) {
                return new SubsetBounds(false, upperBound, null);
            }
            minQuotePerLane[l] = minQ;
        }

        for (int l = numLanes - 1; l >= 0; l--) {
            suffixMinSum[l] = suffixMinSum[l + 1] + minQuotePerLane[l];
        }

        BacktrackState state = new BacktrackState(upperBound);
        int[] currentAssignment = new int[numLanes];

        backtrack(0, currentAssignment, 0, 0L, subset, matrix, suffixMinSum, targetMask, state);

        return new SubsetBounds(state.foundNewBest, state.bestCost, state.bestAssignment);
    }

    private static class BacktrackState {
        Long bestCost;
        int[] bestAssignment;
        boolean foundNewBest;

        BacktrackState(Long initialBestCost) {
            this.bestCost = initialBestCost;
            this.bestAssignment = null;
            this.foundNewBest = false;
        }
    }

    private void backtrack(
            int laneIdx,
            int[] assignment,
            int usedMask,
            long runningCost,
            int[] subset,
            QuoteMatrix matrix,
            long[] suffixMinSum,
            int targetMask,
            BacktrackState state
    ) {
        nodesExplored++;
        if (nodesExplored >= nodeBudget) {
            budgetExhausted = true;
            return;
        }

        // PRUNE 1 — Running cost already >= bestCost
        if (state.bestCost != null && runningCost >= state.bestCost) {
            return;
        }

        int numLanes = matrix.numLanes();

        // LEAF — all lanes assigned
        if (laneIdx == numLanes) {
            if (usedMask == targetMask) {
                if (state.bestCost == null || runningCost < state.bestCost) {
                    state.bestCost = runningCost;
                    state.bestAssignment = Arrays.copyOf(assignment, assignment.length);
                    state.foundNewBest = true;
                }
            }
            return;
        }

        // PRUNE 2 — Pigeonhole: remaining lanes < unused transporters
        int remainingLanes = numLanes - laneIdx;
        int unusedTransporters = subset.length - Integer.bitCount(usedMask);
        if (remainingLanes < unusedTransporters) {
            return;
        }

        // PRUNE 3 — Lower bound
        long lowerBound = suffixMinSum[laneIdx];
        if (state.bestCost != null && runningCost + lowerBound >= state.bestCost) {
            return;
        }

        // BRANCH — try each transporter in subset in ascending ID order
        for (int i = 0; i < subset.length; i++) {
            int t = subset[i];
            if (matrix.hasQuote(t, laneIdx)) {
                assignment[laneIdx] = t;
                backtrack(
                        laneIdx + 1,
                        assignment,
                        usedMask | (1 << i),
                        runningCost + matrix.getQuote(t, laneIdx),
                        subset,
                        matrix,
                        suffixMinSum,
                        targetMask,
                        state
                );
                if (budgetExhausted) {
                    return;
                }
            }
        }
    }

    /**
     * Generates all C(n, k) combinations in lexicographical order.
     */
    static List<int[]> generateCombinations(int n, int k) {
        List<int[]> result = new ArrayList<>();
        if (k <= 0 || k > n) {
            return result;
        }
        int[] comb = new int[k];
        for (int i = 0; i < k; i++) {
            comb[i] = i;
        }
        while (true) {
            result.add(Arrays.copyOf(comb, k));
            int i = k - 1;
            while (i >= 0 && comb[i] == n - k + i) {
                i--;
            }
            if (i < 0) {
                break;
            }
            comb[i]++;
            for (int j = i + 1; j < k; j++) {
                comb[j] = comb[j - 1] + 1;
            }
        }
        return result;
    }
}
