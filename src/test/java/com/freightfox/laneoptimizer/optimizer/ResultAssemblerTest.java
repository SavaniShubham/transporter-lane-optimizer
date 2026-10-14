package com.freightfox.laneoptimizer.optimizer;

import com.freightfox.laneoptimizer.domain.OptimizationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ResultAssembler Unit Tests")
class ResultAssemblerTest {

    @Test
    @DisplayName("Should assemble valid assignment and compute total cost and selected transporters")
    void shouldAssembleValidAssignment() {
        List<Long> transporters = List.of(101L, 102L);
        List<Long> lanes = List.of(1L, 2L, 3L);
        List<QuoteMatrix.RawQuote> quotes = List.of(
                new QuoteMatrix.RawQuote(101L, 1L, 1000L),
                new QuoteMatrix.RawQuote(102L, 2L, 2000L),
                new QuoteMatrix.RawQuote(101L, 3L, 3000L)
        );

        QuoteMatrix matrix = QuoteMatrix.build(transporters, lanes, quotes);

        // Lane 0 (1L) -> T idx 0 (101L), Lane 1 (2L) -> T idx 1 (102L), Lane 2 (3L) -> T idx 0 (101L)
        int[] assignment = new int[]{0, 1, 0};
        OptimizationResult result = ResultAssembler.assemble(assignment, matrix);

        assertEquals(6000L, result.totalCost());
        assertEquals(3, result.assignments().size());
        assertEquals(List.of(101L, 102L), result.selectedTransporterIds());
    }

    @Test
    @DisplayName("Should throw exception if assignment uses a transporter with no quote for that lane")
    void shouldThrowIfTransporterHasNoQuote() {
        List<Long> transporters = List.of(101L, 102L);
        List<Long> lanes = List.of(1L);
        List<QuoteMatrix.RawQuote> quotes = List.of(
                new QuoteMatrix.RawQuote(101L, 1L, 1000L)
        );

        QuoteMatrix matrix = QuoteMatrix.build(transporters, lanes, quotes);

        // Assign lane 0 to T idx 1 (102L), which has no quote
        int[] assignment = new int[]{1};
        assertThrows(IllegalStateException.class, () -> ResultAssembler.assemble(assignment, matrix));
    }
}
