package com.laneoptimizer.optimizer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QuoteMatrix Unit Tests")
class QuoteMatrixTest {

    @Test
    @DisplayName("Should build matrix correctly and query quotes and sentinels")
    void shouldBuildMatrixCorrectly() {
        List<Long> transporters = List.of(2L, 1L); // Unsorted input
        List<Long> lanes = List.of(20L, 10L);      // Unsorted input
        List<QuoteMatrix.RawQuote> quotes = List.of(
                new QuoteMatrix.RawQuote(1L, 10L, 500L),
                new QuoteMatrix.RawQuote(2L, 20L, 750L)
        );

        QuoteMatrix matrix = QuoteMatrix.build(transporters, lanes, quotes);

        assertEquals(2, matrix.numTransporters());
        assertEquals(2, matrix.numLanes());

        // Sorted transporter indices: idx 0 -> 1L, idx 1 -> 2L
        assertEquals(1L, matrix.getTransporterId(0));
        assertEquals(2L, matrix.getTransporterId(1));

        // Sorted lane indices: idx 0 -> 10L, idx 1 -> 20L
        assertEquals(10L, matrix.getLaneId(0));
        assertEquals(20L, matrix.getLaneId(1));

        // Quotes
        assertEquals(500L, matrix.getQuote(0, 0));
        assertTrue(matrix.hasQuote(0, 0));

        assertEquals(QuoteMatrix.NO_QUOTE, matrix.getQuote(0, 1));
        assertFalse(matrix.hasQuote(0, 1));

        assertEquals(750L, matrix.getQuote(1, 1));
        assertTrue(matrix.hasQuote(1, 1));
    }

    @Test
    @DisplayName("Should detect orphan lanes with zero quotes from any transporter")
    void shouldDetectOrphanLanes() {
        List<Long> transporters = List.of(1L, 2L);
        List<Long> lanes = List.of(10L, 20L, 30L);
        List<QuoteMatrix.RawQuote> quotes = List.of(
                new QuoteMatrix.RawQuote(1L, 10L, 100L),
                new QuoteMatrix.RawQuote(2L, 10L, 150L)
        );

        QuoteMatrix matrix = QuoteMatrix.build(transporters, lanes, quotes);

        List<Long> orphanLanes = matrix.lanesWithoutAnyQuote();
        assertEquals(2, orphanLanes.size());
        assertTrue(orphanLanes.contains(20L));
        assertTrue(orphanLanes.contains(30L));
    }
}
