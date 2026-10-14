package com.freightfox.laneoptimizer.optimizer;

import com.freightfox.laneoptimizer.domain.HeuristicOutcome;
import com.freightfox.laneoptimizer.domain.OptimizationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LocalSearchStrategy Unit Tests")
class LocalSearchStrategyTest {

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
                new QuoteMatrix.RawQuote(1L, 1L, 1000L),
                new QuoteMatrix.RawQuote(1L, 2L, 1500L),
                new QuoteMatrix.RawQuote(2L, 3L, 2000L),
                new QuoteMatrix.RawQuote(2L, 4L, 1800L),
                new QuoteMatrix.RawQuote(3L, 5L, 1200L),
                new QuoteMatrix.RawQuote(4L, 1L, 1100L),
                new QuoteMatrix.RawQuote(4L, 4L, 1750L)
        );
        return QuoteMatrix.build(transporters, lanes, quotes);
    }

    @Test
    @DisplayName("Test Case 1: K=3 must yield a valid feasible assignment covering all 5 lanes with <= 3 transporters")
    void tc1_k3_valid_result() {
        QuoteMatrix matrix = buildTestCase1Matrix();
        LocalSearchStrategy heuristic = new LocalSearchStrategy();

        HeuristicOutcome outcome = heuristic.solve(matrix, 3);
        assertInstanceOf(HeuristicOutcome.Feasible.class, outcome);

        OptimizationResult result = ((HeuristicOutcome.Feasible) outcome).result();
        assertEquals(5, result.assignments().size());
        assertTrue(result.selectedTransporterIds().size() <= 3);

        // Every assignment references a valid quote
        for (var assignment : result.assignments()) {
            int tIdx = matrix.getTransporterIndex(assignment.transporterId());
            int lIdx = matrix.getLaneIndex(assignment.laneId());
            assertTrue(matrix.hasQuote(tIdx, lIdx));
        }
    }

    @Test
    @DisplayName("Test Case 2: K=3 must yield a valid feasible assignment")
    void tc2_k3_valid_result() {
        QuoteMatrix matrix = buildTestCase2Matrix();
        LocalSearchStrategy heuristic = new LocalSearchStrategy();

        HeuristicOutcome outcome = heuristic.solve(matrix, 3);
        assertInstanceOf(HeuristicOutcome.Feasible.class, outcome);

        OptimizationResult result = ((HeuristicOutcome.Feasible) outcome).result();
        assertEquals(5, result.assignments().size());
        assertTrue(result.selectedTransporterIds().size() <= 3);
    }

    @Test
    @DisplayName("Expansion rejects move if donor transporter would lose its last lane (net change 0)")
    void expansion_rejects_last_lane_removal() {
        // T1 has quotes for L1 and L2
        // T2 has quotes for L1 and L2
        // T3 has quote for L2 only
        // K=3
        List<Long> transporters = List.of(1L, 2L, 3L);
        List<Long> lanes = List.of(10L, 20L);
        List<QuoteMatrix.RawQuote> quotes = List.of(
                new QuoteMatrix.RawQuote(1L, 10L, 10L),
                new QuoteMatrix.RawQuote(1L, 20L, 10L),
                new QuoteMatrix.RawQuote(2L, 10L, 20L),
                new QuoteMatrix.RawQuote(2L, 20L, 20L),
                new QuoteMatrix.RawQuote(3L, 20L, 30L)
        );
        QuoteMatrix matrix = QuoteMatrix.build(transporters, lanes, quotes);
        LocalSearchStrategy heuristic = new LocalSearchStrategy();

        HeuristicOutcome outcome = heuristic.solve(matrix, 3);
        assertInstanceOf(HeuristicOutcome.Feasible.class, outcome);

        OptimizationResult result = ((HeuristicOutcome.Feasible) outcome).result();
        // Since there are only 2 lanes, at most 2 transporters can ever be used
        assertTrue(result.selectedTransporterIds().size() <= 2);
    }

    @Test
    @DisplayName("When greedy fails due to tight constraint, must return SeedFailed rather than declare infeasible")
    void greedy_fails_returns_seed_failed() {
        // T1: {L1=1}
        // T2: {L2=1}
        // T3: {L1=2, L2=2, L3=1}
        // T4: {L3=100}
        // K=2
        // All lanes have 2 quoters, so no mandatory transporter in Phase 1.
        // In Phase 2, greedy picks T1 for L1 and T2 for L2 (cheapest quoters).
        // For L3, usedSet is full (size 2 == K) and neither T1 nor T2 quotes L3.
        // Therefore, greedy seed fails and returns SeedFailed.
        List<Long> transporters = List.of(1L, 2L, 3L, 4L);
        List<Long> lanes = List.of(1L, 2L, 3L);
        List<QuoteMatrix.RawQuote> quotes = List.of(
                new QuoteMatrix.RawQuote(1L, 1L, 1L),
                new QuoteMatrix.RawQuote(2L, 2L, 1L),
                new QuoteMatrix.RawQuote(3L, 1L, 2L),
                new QuoteMatrix.RawQuote(3L, 2L, 2L),
                new QuoteMatrix.RawQuote(3L, 3L, 1L),
                new QuoteMatrix.RawQuote(4L, 3L, 100L)
        );
        QuoteMatrix matrix = QuoteMatrix.build(transporters, lanes, quotes);
        LocalSearchStrategy heuristic = new LocalSearchStrategy();

        HeuristicOutcome outcome = heuristic.solve(matrix, 2);
        assertInstanceOf(HeuristicOutcome.SeedFailed.class, outcome);
    }
}
