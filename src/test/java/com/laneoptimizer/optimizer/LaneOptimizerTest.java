package com.laneoptimizer.optimizer;

import com.laneoptimizer.domain.ExactOutcome;
import com.laneoptimizer.domain.HeuristicOutcome;
import com.laneoptimizer.domain.OptimizationResult;
import com.laneoptimizer.exception.InfeasibleAssignmentException;
import com.laneoptimizer.exception.SolverBudgetExceededException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LaneOptimizer Facade Unit Tests")
class LaneOptimizerTest {

    // Test Stub for ExhaustiveStrategy
    private static class StubExhaustiveStrategy extends ExhaustiveStrategy {
        private final Queue<ExactOutcome> outcomes = new LinkedList<>();

        StubExhaustiveStrategy(ExactOutcome... outcomes) {
            this.outcomes.addAll(List.of(outcomes));
        }

        @Override
        public ExactOutcome solve(QuoteMatrix matrix, int maxTransporters) {
            return outcomes.isEmpty() ? new ExactOutcome.Infeasible(List.of()) : outcomes.poll();
        }
    }

    // Test Stub for LocalSearchStrategy
    private static class StubLocalSearchStrategy extends LocalSearchStrategy {
        private final HeuristicOutcome outcome;

        StubLocalSearchStrategy(HeuristicOutcome outcome) {
            this.outcome = outcome;
        }

        @Override
        public HeuristicOutcome solve(QuoteMatrix matrix, int maxTransporters) {
            return outcome;
        }
    }

    @Test
    @DisplayName("Combinations math C(n, k) and totalSubsets correctness")
    void combinations_math() {
        assertEquals(35L, LaneOptimizer.combinations(7, 3));
        assertEquals(3003L, LaneOptimizer.combinations(15, 10));
        assertEquals(1L, LaneOptimizer.combinations(0, 0));
        assertEquals(0L, LaneOptimizer.combinations(5, 6));

        // totalSubsets(7, 3, 5): sum of C(7, 1) + C(7, 2) + C(7, 3) = 7 + 21 + 35 = 63
        assertEquals(63L, LaneOptimizer.totalSubsets(7, 3, 5));
        assertTrue(LaneOptimizer.totalSubsets(7, 3, 5) < ExhaustiveStrategy.SUBSET_THRESHOLD);

        // totalSubsets(25, 10, 20): large combinations > 50,000
        assertTrue(LaneOptimizer.totalSubsets(25, 10, 20) > ExhaustiveStrategy.SUBSET_THRESHOLD);

        // Huge combinations that would overflow 64-bit integer are safely capped at Long.MAX_VALUE
        assertEquals(Long.MAX_VALUE, LaneOptimizer.combinations(100, 50));
        assertEquals(Long.MAX_VALUE, LaneOptimizer.totalSubsets(100, 50, 50));
    }

    @Test
    @DisplayName("Pre-check: orphan lane throws InfeasibleAssignmentException with lane IDs")
    void orphan_lane_throws_with_ids() {
        List<Long> transporters = List.of(1L);
        List<Long> lanes = List.of(10L, 20L); // Lane 20 has no quotes
        List<QuoteMatrix.RawQuote> quotes = List.of(new QuoteMatrix.RawQuote(1L, 10L, 500L));
        QuoteMatrix matrix = QuoteMatrix.build(transporters, lanes, quotes);

        LaneOptimizer optimizer = new LaneOptimizer();

        InfeasibleAssignmentException ex = assertThrows(
                InfeasibleAssignmentException.class,
                () -> optimizer.optimize(matrix, 1)
        );
        assertTrue(ex.getUncoveredLaneIds().contains(20L));
    }

    @Test
    @DisplayName("Small instance triggers exact search and returns optimal=true")
    void selects_exact_for_small() {
        List<Long> transporters = List.of(1L, 2L);
        List<Long> lanes = List.of(10L);
        List<QuoteMatrix.RawQuote> quotes = List.of(
                new QuoteMatrix.RawQuote(1L, 10L, 500L),
                new QuoteMatrix.RawQuote(2L, 10L, 600L)
        );
        QuoteMatrix matrix = QuoteMatrix.build(transporters, lanes, quotes);

        LaneOptimizer optimizer = new LaneOptimizer();
        LaneOptimizer.FinalResult result = optimizer.optimize(matrix, 1);

        assertTrue(result.optimal());
        assertEquals(500L, result.result().totalCost());
        assertEquals(List.of(1L), result.result().selectedTransporterIds());
    }

    @Test
    @DisplayName("Budget exhaustion Exhausted(null) falls back to heuristic")
    void budget_exhaustion_falls_back_to_heuristic() {
        QuoteMatrix matrix = QuoteMatrix.build(List.of(1L), List.of(10L), List.of(new QuoteMatrix.RawQuote(1L, 10L, 100L)));
        OptimizationResult heuristicResult = ResultAssembler.assemble(new int[]{0}, matrix);

        StubExhaustiveStrategy stubExact = new StubExhaustiveStrategy(new ExactOutcome.Exhausted(null));
        StubLocalSearchStrategy stubHeuristic = new StubLocalSearchStrategy(new HeuristicOutcome.Feasible(heuristicResult));

        LaneOptimizer optimizer = new LaneOptimizer(stubExact, stubHeuristic);
        LaneOptimizer.FinalResult finalResult = optimizer.optimize(matrix, 1);

        assertFalse(finalResult.optimal());
        assertEquals(100L, finalResult.result().totalCost());
    }

    @Test
    @DisplayName("Heuristic SeedFailed falls back to exact solver")
    void heuristic_seed_failure_falls_back_to_exact() {
        QuoteMatrix matrix = QuoteMatrix.build(List.of(1L), List.of(10L), List.of(new QuoteMatrix.RawQuote(1L, 10L, 100L)));
        OptimizationResult exactResult = ResultAssembler.assemble(new int[]{0}, matrix);

        // Step 3 exact returns Exhausted(null), heuristic returns SeedFailed, step 5 exact returns Optimal
        StubExhaustiveStrategy stubExact = new StubExhaustiveStrategy(
                new ExactOutcome.Exhausted(null),
                new ExactOutcome.Optimal(exactResult)
        );
        StubLocalSearchStrategy stubHeuristic = new StubLocalSearchStrategy(new HeuristicOutcome.SeedFailed());

        LaneOptimizer optimizer = new LaneOptimizer(stubExact, stubHeuristic);
        LaneOptimizer.FinalResult finalResult = optimizer.optimize(matrix, 1);

        assertTrue(finalResult.optimal());
        assertEquals(100L, finalResult.result().totalCost());
    }

    @Test
    @DisplayName("Critical: Exhausted(null) never becomes 422! Throws 503 SolverBudgetExceededException")
    void exhausted_null_never_returns_422() {
        QuoteMatrix matrix = QuoteMatrix.build(List.of(1L), List.of(10L), List.of(new QuoteMatrix.RawQuote(1L, 10L, 100L)));

        StubExhaustiveStrategy stubExact = new StubExhaustiveStrategy(
                new ExactOutcome.Exhausted(null),
                new ExactOutcome.Exhausted(null)
        );
        StubLocalSearchStrategy stubHeuristic = new StubLocalSearchStrategy(new HeuristicOutcome.SeedFailed());

        LaneOptimizer optimizer = new LaneOptimizer(stubExact, stubHeuristic);

        assertThrows(SolverBudgetExceededException.class, () -> optimizer.optimize(matrix, 1));
    }

    @Test
    @DisplayName("Proven infeasibility after complete search throws InfeasibleAssignmentException (422)")
    void proven_infeasible_returns_422() {
        QuoteMatrix matrix = QuoteMatrix.build(List.of(1L), List.of(10L), List.of(new QuoteMatrix.RawQuote(1L, 10L, 100L)));

        StubExhaustiveStrategy stubExact = new StubExhaustiveStrategy(new ExactOutcome.Infeasible(List.of(10L)));
        StubLocalSearchStrategy stubHeuristic = new StubLocalSearchStrategy(new HeuristicOutcome.SeedFailed());

        LaneOptimizer optimizer = new LaneOptimizer(stubExact, stubHeuristic);

        assertThrows(InfeasibleAssignmentException.class, () -> optimizer.optimize(matrix, 1));
    }
}
