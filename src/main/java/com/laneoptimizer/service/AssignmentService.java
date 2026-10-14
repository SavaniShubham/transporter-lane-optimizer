package com.laneoptimizer.service;

import com.laneoptimizer.domain.OptimizationResult;
import com.laneoptimizer.dto.AssignmentItem;
import com.laneoptimizer.dto.AssignmentRequest;
import com.laneoptimizer.dto.AssignmentResponse;
import com.laneoptimizer.entity.LaneEntity;
import com.laneoptimizer.entity.LaneQuoteEntity;
import com.laneoptimizer.entity.TransporterEntity;
import com.laneoptimizer.exception.NoInputDataException;
import com.laneoptimizer.optimizer.LaneOptimizer;
import com.laneoptimizer.optimizer.QuoteMatrix;
import com.laneoptimizer.repository.LaneQuoteRepository;
import com.laneoptimizer.repository.LaneRepository;
import com.laneoptimizer.repository.TransporterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing lane assignment optimization requests.
 */
@Service
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final LaneRepository laneRepository;
    private final TransporterRepository transporterRepository;
    private final LaneQuoteRepository laneQuoteRepository;
    private final LaneOptimizer laneOptimizer;

    public AssignmentService(
            LaneRepository laneRepository,
            TransporterRepository transporterRepository,
            LaneQuoteRepository laneQuoteRepository,
            LaneOptimizer laneOptimizer
    ) {
        this.laneRepository = laneRepository;
        this.transporterRepository = transporterRepository;
        this.laneQuoteRepository = laneQuoteRepository;
        this.laneOptimizer = laneOptimizer;
    }

    /**
     * Executes the lane assignment optimization against the currently stored dataset.
     *
     * @param request request containing the maxTransporters constraint
     * @return AssignmentResponse detailing assigned lanes, total cost, and distinct transporters
     */
    @Transactional(readOnly = true)
    public AssignmentResponse calculateAssignment(AssignmentRequest request) {
        List<LaneEntity> lanes = laneRepository.findAll();
        List<TransporterEntity> transporters = transporterRepository.findAll();
        List<LaneQuoteEntity> quotes = laneQuoteRepository.findAll();

        if (lanes.isEmpty() || transporters.isEmpty() || quotes.isEmpty()) {
            throw new NoInputDataException();
        }

        List<Long> laneIds = lanes.stream().map(LaneEntity::getId).toList();
        List<Long> transporterIds = transporters.stream().map(TransporterEntity::getId).toList();

        List<QuoteMatrix.RawQuote> rawQuotes = quotes.stream()
                .map(q -> new QuoteMatrix.RawQuote(q.getTransporterId(), q.getLaneId(), q.getQuote()))
                .toList();

        QuoteMatrix matrix = QuoteMatrix.build(transporterIds, laneIds, rawQuotes);

        log.info("Calculating optimal lane assignment: {} lanes, {} transporters, maxTransporters={}",
                laneIds.size(), transporterIds.size(), request.maxTransporters());

        LaneOptimizer.FinalResult finalResult = laneOptimizer.optimize(matrix, request.maxTransporters());
        OptimizationResult optResult = finalResult.result();

        log.info("Assignment calculation completed: totalCost={}, optimal={}, distinctTransporters={}",
                optResult.totalCost(), finalResult.optimal(), optResult.selectedTransporterIds().size());

        List<AssignmentItem> assignmentItems = optResult.assignments().stream()
                .map(a -> new AssignmentItem(a.laneId(), a.transporterId(), a.cost()))
                .toList();

        return new AssignmentResponse(
                "success",
                optResult.totalCost(),
                finalResult.optimal(),
                assignmentItems,
                optResult.selectedTransporterIds()
        );
    }
}
