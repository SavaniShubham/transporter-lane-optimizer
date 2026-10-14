package com.laneoptimizer.optimizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dense cost matrix representing transporter quotes across trade lanes.
 * Uses primitive arrays for cache-friendly and high-performance evaluations.
 * Pure Java: no framework or Spring imports.
 */
public final class QuoteMatrix {

    public static final long NO_QUOTE = -1L;

    private final long[] transporterIds;
    private final long[] laneIds;
    private final long[][] matrix; // [transporterIndex][laneIndex]
    private final Map<Long, Integer> transporterIdToIndex;
    private final Map<Long, Integer> laneIdToIndex;

    public QuoteMatrix(long[] transporterIds, long[] laneIds, long[][] matrix) {
        if (transporterIds == null || laneIds == null || matrix == null) {
            throw new IllegalArgumentException("Inputs to QuoteMatrix cannot be null");
        }
        this.transporterIds = Arrays.copyOf(transporterIds, transporterIds.length);
        this.laneIds = Arrays.copyOf(laneIds, laneIds.length);

        this.matrix = new long[transporterIds.length][laneIds.length];
        for (int i = 0; i < transporterIds.length; i++) {
            System.arraycopy(matrix[i], 0, this.matrix[i], 0, laneIds.length);
        }

        this.transporterIdToIndex = new HashMap<>(transporterIds.length);
        for (int i = 0; i < transporterIds.length; i++) {
            transporterIdToIndex.put(transporterIds[i], i);
        }

        this.laneIdToIndex = new HashMap<>(laneIds.length);
        for (int j = 0; j < laneIds.length; j++) {
            laneIdToIndex.put(laneIds[j], j);
        }
    }

    /**
     * Factory helper to build a QuoteMatrix from structured quote records.
     */
    public static QuoteMatrix build(
            List<Long> distinctTransporterIds,
            List<Long> distinctLaneIds,
            List<RawQuote> quotes
    ) {
        List<Long> sortedTransporters = new ArrayList<>(distinctTransporterIds);
        Collections.sort(sortedTransporters);

        List<Long> sortedLanes = new ArrayList<>(distinctLaneIds);
        Collections.sort(sortedLanes);

        long[] tIds = sortedTransporters.stream().mapToLong(Long::longValue).toArray();
        long[] lIds = sortedLanes.stream().mapToLong(Long::longValue).toArray();

        Map<Long, Integer> tMap = new HashMap<>();
        for (int i = 0; i < tIds.length; i++) {
            tMap.put(tIds[i], i);
        }
        Map<Long, Integer> lMap = new HashMap<>();
        for (int j = 0; j < lIds.length; j++) {
            lMap.put(lIds[j], j);
        }

        long[][] mat = new long[tIds.length][lIds.length];
        for (long[] row : mat) {
            Arrays.fill(row, NO_QUOTE);
        }

        for (RawQuote q : quotes) {
            Integer tIdx = tMap.get(q.transporterId());
            Integer lIdx = lMap.get(q.laneId());
            if (tIdx != null && lIdx != null) {
                mat[tIdx][lIdx] = q.cost();
            }
        }

        return new QuoteMatrix(tIds, lIds, mat);
    }

    public int numTransporters() {
        return transporterIds.length;
    }

    public int numLanes() {
        return laneIds.length;
    }

    public long getTransporterId(int transporterIndex) {
        return transporterIds[transporterIndex];
    }

    public long getLaneId(int laneIndex) {
        return laneIds[laneIndex];
    }

    public int getTransporterIndex(long transporterId) {
        Integer idx = transporterIdToIndex.get(transporterId);
        return idx != null ? idx : -1;
    }

    public int getLaneIndex(long laneId) {
        Integer idx = laneIdToIndex.get(laneId);
        return idx != null ? idx : -1;
    }

    public long getQuote(int transporterIndex, int laneIndex) {
        return matrix[transporterIndex][laneIndex];
    }

    public boolean hasQuote(int transporterIndex, int laneIndex) {
        return matrix[transporterIndex][laneIndex] != NO_QUOTE;
    }

    /**
     * Identifies any orphan lanes that have zero valid quotes from any transporter.
     *
     * @return list of lane IDs with no quotes
     */
    public List<Long> lanesWithoutAnyQuote() {
        List<Long> orphanLaneIds = new ArrayList<>();
        for (int j = 0; j < laneIds.length; j++) {
            boolean hasAtLeastOneQuote = false;
            for (int i = 0; i < transporterIds.length; i++) {
                if (matrix[i][j] != NO_QUOTE) {
                    hasAtLeastOneQuote = true;
                    break;
                }
            }
            if (!hasAtLeastOneQuote) {
                orphanLaneIds.add(laneIds[j]);
            }
        }
        return Collections.unmodifiableList(orphanLaneIds);
    }

    public record RawQuote(long transporterId, long laneId, long cost) {}
}
