package com.laneoptimizer.optimizer;

import com.laneoptimizer.domain.ExactOutcome;
import com.laneoptimizer.domain.OptimizationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExhaustiveStrategy Unit Tests")
class ExhaustiveStrategyTest {

    private QuoteMatrix buildTestCase1Matrix() {
        List<Long> transporters = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L);
        List<Long> lanes = List.of(1L, 2L, 3L, 4L, 5L);
        long[][] table = {
                {20835, 48844, 39020, 14400, 11601, 35095, 26070}, // Lane 1
                {10512, 31326, 20648, 44514, 19760, 12494, 41098}, // Lane 2
                {22105, 18640, 31438, 14316, 40870, 17808, 20932}, // Lane 3
                {42481, 45828, 36447, 10678, 20635, 36210, 16897}, // Lane 4
                {19862, 18297, 12789, 13032, 26421, 39444, 27938}  // Lane 5
        };

        List<QuoteMatrix.RawQuote> quotes = new ArrayList<>();
        for (int l = 0; l < 5; l++) {
            for (int t = 0; t < 7; t++) {
                quotes.add(new QuoteMatrix.RawQuote((long) (t + 1), (long) (l + 1), table[l][t]));
            }
        }
        return QuoteMatrix.build(transporters, lanes, quotes);
    }

    private QuoteMatrix buildTestCase2Matrix() {
        List<Long> transporters = List.of(1L, 2L, 3L, 4L); // X=1, Y=2, Z=3, W=4
        List<Long> lanes = List.of(1L, 2L, 3L, 4L, 5L);
        List<QuoteMatrix.RawQuote> quotes = List.of(
                new QuoteMatrix.RawQuote(1L, 1L, 1000L), // X: L1=1000, L2=1500
                new QuoteMatrix.RawQuote(1L, 2L, 1500L),
                new QuoteMatrix.RawQuote(2L, 3L, 2000L), // Y: L3=2000, L4=1800
                new QuoteMatrix.RawQuote(2L, 4L, 1800L),
                new QuoteMatrix.RawQuote(3L, 5L, 1200L), // Z: L5=1200
                new QuoteMatrix.RawQuote(4L, 1L, 1100L), // W: L1=1100, L4=1750
                new QuoteMatrix.RawQuote(4L, 4L, 1750L)
        );
        return QuoteMatrix.build(transporters, lanes, quotes);
    }

    @Test
    @DisplayName("Test Case 1: K=3 must yield proven optimal cost 60139 with transporters {1, 4, 5}")
    void tc1_k3_exact() {
        QuoteMatrix matrix = buildTestCase1Matrix();
        ExhaustiveStrategy solver = new ExhaustiveStrategy();

        ExactOutcome outcome = solver.solve(matrix, 3);
        assertInstanceOf(ExactOutcome.Optimal.class, outcome);

        OptimizationResult result = ((ExactOutcome.Optimal) outcome).result();
        assertEquals(60139L, result.totalCost());
        assertEquals(List.of(1L, 4L, 5L), result.selectedTransporterIds());
        assertEquals(3, result.selectedTransporterIds().size());
    }

    @Test
    @DisplayName("Test Case 1: K=5 must yield cost 63388 with transporters {1, 3, 4, 5, 6} under Interpretation B")
    void tc1_k5_exact() {
        QuoteMatrix matrix = buildTestCase1Matrix();
        ExhaustiveStrategy solver = new ExhaustiveStrategy();

        ExactOutcome outcome = solver.solve(matrix, 5);
        assertInstanceOf(ExactOutcome.Optimal.class, outcome);

        OptimizationResult result = ((ExactOutcome.Optimal) outcome).result();
        assertEquals(63388L, result.totalCost());
        assertEquals(List.of(1L, 3L, 4L, 5L, 6L), result.selectedTransporterIds());
        assertEquals(5, result.selectedTransporterIds().size());
    }

    @Test
    @DisplayName("Test Case 1: K=7 pigeonhole cap must yield usage 5 and cost 63388")
    void tc1_k7_exact() {
        QuoteMatrix matrix = buildTestCase1Matrix();
        ExhaustiveStrategy solver = new ExhaustiveStrategy();

        ExactOutcome outcome = solver.solve(matrix, 7);
        assertInstanceOf(ExactOutcome.Optimal.class, outcome);

        OptimizationResult result = ((ExactOutcome.Optimal) outcome).result();
        assertEquals(63388L, result.totalCost());
        assertEquals(5, result.selectedTransporterIds().size());
    }

    @Test
    @DisplayName("Test Case 2: K=3 must yield cost 7500 with transporters {1, 2, 3}")
    void tc2_k3_exact() {
        QuoteMatrix matrix = buildTestCase2Matrix();
        ExhaustiveStrategy solver = new ExhaustiveStrategy();

        ExactOutcome outcome = solver.solve(matrix, 3);
        assertInstanceOf(ExactOutcome.Optimal.class, outcome);

        OptimizationResult result = ((ExactOutcome.Optimal) outcome).result();
        assertEquals(7500L, result.totalCost());
        assertEquals(List.of(1L, 2L, 3L), result.selectedTransporterIds());
    }

    @Test
    @DisplayName("Test Case 2: K=4 must yield cost 7450 with transporters {1, 2, 3, 4}")
    void tc2_k4_exact() {
        QuoteMatrix matrix = buildTestCase2Matrix();
        ExhaustiveStrategy solver = new ExhaustiveStrategy();

        ExactOutcome outcome = solver.solve(matrix, 4);
        assertInstanceOf(ExactOutcome.Optimal.class, outcome);

        OptimizationResult result = ((ExactOutcome.Optimal) outcome).result();
        assertEquals(7450L, result.totalCost());
        assertEquals(List.of(1L, 2L, 3L, 4L), result.selectedTransporterIds());
    }

    @Test
    @DisplayName("Test Case 2: K=2 must prove problem is Infeasible")
    void tc2_k2_infeasible() {
        QuoteMatrix matrix = buildTestCase2Matrix();
        ExhaustiveStrategy solver = new ExhaustiveStrategy();

        ExactOutcome outcome = solver.solve(matrix, 2);
        assertInstanceOf(ExactOutcome.Infeasible.class, outcome);
    }

    @Test
    @DisplayName("Single lane, single transporter minimal case")
    void single_lane_single_transporter() {
        List<Long> transporters = List.of(10L);
        List<Long> lanes = List.of(5L);
        List<QuoteMatrix.RawQuote> quotes = List.of(new QuoteMatrix.RawQuote(10L, 5L, 4200L));
        QuoteMatrix matrix = QuoteMatrix.build(transporters, lanes, quotes);

        ExhaustiveStrategy solver = new ExhaustiveStrategy();
        ExactOutcome outcome = solver.solve(matrix, 1);
        assertInstanceOf(ExactOutcome.Optimal.class, outcome);

        OptimizationResult result = ((ExactOutcome.Optimal) outcome).result();
        assertEquals(4200L, result.totalCost());
        assertEquals(List.of(10L), result.selectedTransporterIds());
    }

    @Test
    @DisplayName("Equal-cost ties should be broken deterministically by smaller transporter ID")
    void equal_cost_ties() {
        List<Long> transporters = List.of(1L, 2L);
        List<Long> lanes = List.of(10L, 20L);
        List<QuoteMatrix.RawQuote> quotes = List.of(
                new QuoteMatrix.RawQuote(1L, 10L, 100L),
                new QuoteMatrix.RawQuote(1L, 20L, 200L),
                new QuoteMatrix.RawQuote(2L, 10L, 100L),
                new QuoteMatrix.RawQuote(2L, 20L, 200L)
        );
        QuoteMatrix matrix = QuoteMatrix.build(transporters, lanes, quotes);

        ExhaustiveStrategy solver = new ExhaustiveStrategy();
        ExactOutcome outcome = solver.solve(matrix, 2);
        assertInstanceOf(ExactOutcome.Optimal.class, outcome);

        OptimizationResult result = ((ExactOutcome.Optimal) outcome).result();
        assertEquals(300L, result.totalCost());
        assertEquals(List.of(1L, 2L), result.selectedTransporterIds());
        // For Lane 10, both quote 100. T1 has smaller ID, so L10 -> T1, and L20 -> T2 to achieve usage 2.
        assertEquals(1L, result.assignments().get(0).transporterId());
        assertEquals(2L, result.assignments().get(1).transporterId());
    }

    @Test
    @DisplayName("Multiple lanes served by a single transporter (K=1)")
    void multiple_lanes_single_transporter() {
        List<Long> transporters = List.of(77L);
        List<Long> lanes = List.of(10L, 20L, 30L);
        List<QuoteMatrix.RawQuote> quotes = List.of(
                new QuoteMatrix.RawQuote(77L, 10L, 1000L),
                new QuoteMatrix.RawQuote(77L, 20L, 2000L),
                new QuoteMatrix.RawQuote(77L, 30L, 3000L)
        );
        QuoteMatrix matrix = QuoteMatrix.build(transporters, lanes, quotes);

        ExhaustiveStrategy solver = new ExhaustiveStrategy();
        ExactOutcome outcome = solver.solve(matrix, 1);
        assertInstanceOf(ExactOutcome.Optimal.class, outcome);

        OptimizationResult result = ((ExactOutcome.Optimal) outcome).result();
        assertEquals(6000L, result.totalCost());
        assertEquals(List.of(77L), result.selectedTransporterIds());
        assertEquals(3, result.assignments().size());
    }

    @Test
    @DisplayName("One lane with multiple competing transporters selects the cheapest")
    void one_lane_multiple_transporters() {
        List<Long> transporters = List.of(10L, 20L, 30L);
        List<Long> lanes = List.of(1L);
        List<QuoteMatrix.RawQuote> quotes = List.of(
                new QuoteMatrix.RawQuote(10L, 1L, 5000L),
                new QuoteMatrix.RawQuote(20L, 1L, 3000L),
                new QuoteMatrix.RawQuote(30L, 1L, 4000L)
        );
        QuoteMatrix matrix = QuoteMatrix.build(transporters, lanes, quotes);

        ExhaustiveStrategy solver = new ExhaustiveStrategy();
        ExactOutcome outcome = solver.solve(matrix, 2);
        assertInstanceOf(ExactOutcome.Optimal.class, outcome);

        OptimizationResult result = ((ExactOutcome.Optimal) outcome).result();
        assertEquals(3000L, result.totalCost());
        assertEquals(List.of(20L), result.selectedTransporterIds());
    }

    @Test
    @DisplayName("Non-contiguous IDs and IDs not starting from 1 are handled correctly")
    void non_contiguous_and_offset_ids() {
        List<Long> transporters = List.of(999L, 55L);
        List<Long> lanes = List.of(500L, 120L);
        List<QuoteMatrix.RawQuote> quotes = List.of(
                new QuoteMatrix.RawQuote(55L, 120L, 1500L),
                new QuoteMatrix.RawQuote(55L, 500L, 2500L),
                new QuoteMatrix.RawQuote(999L, 120L, 1200L),
                new QuoteMatrix.RawQuote(999L, 500L, 2200L)
        );
        QuoteMatrix matrix = QuoteMatrix.build(transporters, lanes, quotes);

        ExhaustiveStrategy solver = new ExhaustiveStrategy();
        ExactOutcome outcome = solver.solve(matrix, 2);
        assertInstanceOf(ExactOutcome.Optimal.class, outcome);

        OptimizationResult result = ((ExactOutcome.Optimal) outcome).result();
        // For K=2, usage 2 must be achieved: one lane to 55L, one to 999L
        assertEquals(2, result.selectedTransporterIds().size());
        assertEquals(List.of(55L, 999L), result.selectedTransporterIds());
    }

    @Test
    @DisplayName("Lexicographic Priority 1: Usage maximization strictly preferred over cheaper fewer-transporter solution")
    void usage_maximization_strictly_preferred_over_cheaper_solution() {
        // T1 alone can cover both lanes for 20 (L1=10, L2=10)
        // T2 quotes L1=15, L2=15
        // If K=2, solver must pick usage 2 (cost 10+15=25) over usage 1 (cost 20)
        List<Long> transporters = List.of(1L, 2L);
        List<Long> lanes = List.of(101L, 102L);
        List<QuoteMatrix.RawQuote> quotes = List.of(
                new QuoteMatrix.RawQuote(1L, 101L, 10L),
                new QuoteMatrix.RawQuote(1L, 102L, 10L),
                new QuoteMatrix.RawQuote(2L, 101L, 15L),
                new QuoteMatrix.RawQuote(2L, 102L, 15L)
        );
        QuoteMatrix matrix = QuoteMatrix.build(transporters, lanes, quotes);

        ExhaustiveStrategy solver = new ExhaustiveStrategy();
        ExactOutcome outcome = solver.solve(matrix, 2);
        assertInstanceOf(ExactOutcome.Optimal.class, outcome);

        OptimizationResult result = ((ExactOutcome.Optimal) outcome).result();
        assertEquals(2, result.selectedTransporterIds().size());
        assertEquals(List.of(1L, 2L), result.selectedTransporterIds());
        assertEquals(25L, result.totalCost());
    }

    @Test
    @DisplayName("Quotes with very large values do not overflow 64-bit long arithmetic")
    void large_quote_values() {
        List<Long> transporters = List.of(1L, 2L);
        List<Long> lanes = List.of(1L, 2L);
        List<QuoteMatrix.RawQuote> quotes = List.of(
                new QuoteMatrix.RawQuote(1L, 1L, 3_000_000_000L),
                new QuoteMatrix.RawQuote(2L, 2L, 4_000_000_000L)
        );
        QuoteMatrix matrix = QuoteMatrix.build(transporters, lanes, quotes);

        ExhaustiveStrategy solver = new ExhaustiveStrategy();
        ExactOutcome outcome = solver.solve(matrix, 2);
        assertInstanceOf(ExactOutcome.Optimal.class, outcome);

        OptimizationResult result = ((ExactOutcome.Optimal) outcome).result();
        assertEquals(7_000_000_000L, result.totalCost());
        assertEquals(List.of(1L, 2L), result.selectedTransporterIds());
    }

    @Test
    @DisplayName("Budget exhaustion should return Exhausted, never Infeasible")
    void budget_exhaustion_returns_exhausted() {
        QuoteMatrix matrix = buildTestCase1Matrix();
        // Set an artificially tiny node budget of 2 nodes
        ExhaustiveStrategy solver = new ExhaustiveStrategy(2L);

        ExactOutcome outcome = solver.solve(matrix, 5);
        assertInstanceOf(ExactOutcome.Exhausted.class, outcome);
        assertFalse(outcome instanceof ExactOutcome.Infeasible);
    }
}
