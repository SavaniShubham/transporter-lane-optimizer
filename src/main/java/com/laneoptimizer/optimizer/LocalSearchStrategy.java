package com.laneoptimizer.optimizer;

import com.laneoptimizer.domain.HeuristicOutcome;
import com.laneoptimizer.domain.OptimizationResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Heuristic local search assignment strategy implementing best-effort usage maximization.
 * Never claims optimality or declares infeasibility.
 * If the greedy seed cannot find a feasible assignment, returns SeedFailed.
 * Pure Java: no Spring dependencies.
 */
public class LocalSearchStrategy implements AssignmentStrategy {

    @Override
    public String name() {
        return "LocalSearchStrategy";
    }

    /**
     * Solves the assignment problem using greedy seed + usage expansion + cost improvement swaps.
     *
     * @param matrix dense quote matrix
     * @param maxTransporters maximum allowed distinct transporters (K)
     * @return HeuristicOutcome (Feasible or SeedFailed)
     */
    public HeuristicOutcome solve(QuoteMatrix matrix, int maxTransporters) {
        int numTransporters = matrix.numTransporters();
        int numLanes = matrix.numLanes();

        if (numLanes == 0 || numTransporters == 0 || maxTransporters < 1) {
            return new HeuristicOutcome.SeedFailed();
        }

        // Early check for orphan lanes
        if (!matrix.lanesWithoutAnyQuote().isEmpty()) {
            return new HeuristicOutcome.SeedFailed();
        }

        int[] assignment = new int[numLanes];
        Arrays.fill(assignment, -1);
        int[] transporterLaneCount = new int[numTransporters];
        Set<Integer> usedTransporters = new HashSet<>();

        // Phase 1 — Mandatory transporters (lanes with exactly 1 quoter)
        Set<Integer> mandatoryTransporters = new HashSet<>();
        for (int l = 0; l < numLanes; l++) {
            int quoterCount = 0;
            int singleQuoter = -1;
            for (int t = 0; t < numTransporters; t++) {
                if (matrix.hasQuote(t, l)) {
                    quoterCount++;
                    singleQuoter = t;
                }
            }
            if (quoterCount == 1) {
                mandatoryTransporters.add(singleQuoter);
                assignment[l] = singleQuoter;
                transporterLaneCount[singleQuoter]++;
                usedTransporters.add(singleQuoter);
            }
        }

        if (mandatoryTransporters.size() > maxTransporters) {
            return new HeuristicOutcome.SeedFailed();
        }

        // Phase 2 — Assign remaining lanes (hardest first: fewest quoters)
        List<Integer> unassignedLanes = new ArrayList<>();
        for (int l = 0; l < numLanes; l++) {
            if (assignment[l] == -1) {
                unassignedLanes.add(l);
            }
        }

        unassignedLanes.sort(Comparator.comparingInt(l -> {
            int count = 0;
            for (int t = 0; t < numTransporters; t++) {
                if (matrix.hasQuote(t, l)) {
                    count++;
                }
            }
            return count;
        }));

        for (int l : unassignedLanes) {
            // Check quoters in usedTransporters first
            int bestUsedT = -1;
            long bestUsedCost = Long.MAX_VALUE;
            for (int t : usedTransporters) {
                if (matrix.hasQuote(t, l)) {
                    long cost = matrix.getQuote(t, l);
                    if (cost < bestUsedCost || (cost == bestUsedCost && t < bestUsedT)) {
                        bestUsedCost = cost;
                        bestUsedT = t;
                    }
                }
            }

            if (bestUsedT != -1) {
                assignment[l] = bestUsedT;
                transporterLaneCount[bestUsedT]++;
            } else if (usedTransporters.size() < maxTransporters) {
                // Must add a new transporter
                int bestNewT = -1;
                long bestNewCost = Long.MAX_VALUE;
                for (int t = 0; t < numTransporters; t++) {
                    if (!usedTransporters.contains(t) && matrix.hasQuote(t, l)) {
                        long cost = matrix.getQuote(t, l);
                        if (cost < bestNewCost || (cost == bestNewCost && t < bestNewT)) {
                            bestNewCost = cost;
                            bestNewT = t;
                        }
                    }
                }
                if (bestNewT == -1) {
                    return new HeuristicOutcome.SeedFailed();
                }
                assignment[l] = bestNewT;
                transporterLaneCount[bestNewT]++;
                usedTransporters.add(bestNewT);
            } else {
                // Used capacity full and no used quoter can cover this lane
                return new HeuristicOutcome.SeedFailed();
            }
        }

        // Verify valid greedy seed
        for (int l = 0; l < numLanes; l++) {
            if (assignment[l] == -1 || !matrix.hasQuote(assignment[l], l)) {
                return new HeuristicOutcome.SeedFailed();
            }
        }
        if (usedTransporters.size() > maxTransporters) {
            return new HeuristicOutcome.SeedFailed();
        }

        int targetMaxUsage = Math.min(maxTransporters, Math.min(numTransporters, numLanes));

        // Phase 3 — Expansion Phase (best-effort usage increase)
        // A move is accepted as usage increase IF AND ONLY IF:
        // 1. tNew is not currently in usedTransporters.
        // 2. tOld retains >= 1 other lane (transporterLaneCount[tOld] > 1).
        // 3. verified that usedCount increases by exactly 1.
        boolean expanded = true;
        while (usedTransporters.size() < targetMaxUsage && expanded) {
            expanded = false;

            int bestL = -1;
            int bestTNew = -1;
            long bestCostDiff = Long.MAX_VALUE;

            for (int l = 0; l < numLanes; l++) {
                int tOld = assignment[l];
                if (transporterLaneCount[tOld] <= 1) {
                    // Moving this lane would evict tOld, resulting in net change 0. Reject.
                    continue;
                }

                for (int tNew = 0; tNew < numTransporters; tNew++) {
                    if (!usedTransporters.contains(tNew) && matrix.hasQuote(tNew, l)) {
                        long diff = matrix.getQuote(tNew, l) - matrix.getQuote(tOld, l);
                        if (diff < bestCostDiff || (diff == bestCostDiff && (bestL == -1 || tNew < bestTNew))) {
                            bestCostDiff = diff;
                            bestL = l;
                            bestTNew = tNew;
                        }
                    }
                }
            }

            if (bestL != -1 && bestTNew != -1) {
                int tOld = assignment[bestL];
                assignment[bestL] = bestTNew;
                transporterLaneCount[tOld]--;
                transporterLaneCount[bestTNew]++;
                usedTransporters.add(bestTNew);
                expanded = true;
            }
        }

        // Phase 4 — Cost optimization swaps among currently used transporters
        // Reassign lane L to another used transporter if cheaper, provided donor keeps >= 1 lane
        boolean improved = true;
        int maxSwapIterations = 1000;
        int iteration = 0;

        while (improved && iteration++ < maxSwapIterations) {
            improved = false;
            for (int l = 0; l < numLanes; l++) {
                int tCurrent = assignment[l];
                long currentCost = matrix.getQuote(tCurrent, l);

                for (int tCandidate : usedTransporters) {
                    if (tCandidate != tCurrent && matrix.hasQuote(tCandidate, l)) {
                        long candCost = matrix.getQuote(tCandidate, l);
                        if (candCost < currentCost) {
                            // Can only swap if either tCurrent has another lane OR usage requirement permits
                            // If tCurrent has only 1 lane, evicting it would reduce usedCount!
                            if (transporterLaneCount[tCurrent] > 1) {
                                assignment[l] = tCandidate;
                                transporterLaneCount[tCurrent]--;
                                transporterLaneCount[tCandidate]++;
                                improved = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        OptimizationResult result = ResultAssembler.assemble(assignment, matrix);
        return new HeuristicOutcome.Feasible(result);
    }
}
