package com.freightfox.laneoptimizer.optimizer;

import com.freightfox.laneoptimizer.domain.LaneAssignment;
import com.freightfox.laneoptimizer.domain.OptimizationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class to assemble and validate OptimizationResult objects from raw indices.
 * Package-private, pure Java.
 */
final class ResultAssembler {

    private ResultAssembler() {}

    /**
     * Assembles an OptimizationResult given an array of transporter indices assigned to each lane.
     *
     * @param laneToTransporterIdx array where index is lane index and value is transporter index
     * @param matrix the quote matrix
     * @return fully validated OptimizationResult
     */
    static OptimizationResult assemble(int[] laneToTransporterIdx, QuoteMatrix matrix) {
        if (laneToTransporterIdx == null || matrix == null) {
            throw new IllegalArgumentException("Inputs to ResultAssembler cannot be null");
        }
        int numLanes = matrix.numLanes();
        if (laneToTransporterIdx.length != numLanes) {
            throw new IllegalArgumentException("Assignment array length must match number of lanes");
        }

        long totalCost = 0L;
        List<LaneAssignment> assignments = new ArrayList<>(numLanes);
        Set<Long> uniqueTransporterIds = new HashSet<>();

        for (int laneIdx = 0; laneIdx < numLanes; laneIdx++) {
            int tIdx = laneToTransporterIdx[laneIdx];
            if (tIdx < 0 || tIdx >= matrix.numTransporters()) {
                throw new IllegalStateException("Invalid transporter index assigned to lane: " + tIdx);
            }
            long quote = matrix.getQuote(tIdx, laneIdx);
            if (quote == QuoteMatrix.NO_QUOTE) {
                throw new IllegalStateException("Transporter " + matrix.getTransporterId(tIdx)
                        + " has no valid quote for lane " + matrix.getLaneId(laneIdx));
            }
            totalCost += quote;
            long laneId = matrix.getLaneId(laneIdx);
            long transporterId = matrix.getTransporterId(tIdx);
            uniqueTransporterIds.add(transporterId);
            assignments.add(new LaneAssignment(laneId, transporterId, quote));
        }

        List<Long> selectedTransporters = new ArrayList<>(uniqueTransporterIds);
        Collections.sort(selectedTransporters);

        return new OptimizationResult(totalCost, assignments, selectedTransporters);
    }
}
