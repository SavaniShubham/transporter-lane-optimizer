package com.freightfox.laneoptimizer.service;

import com.freightfox.laneoptimizer.dto.InputRequest;
import com.freightfox.laneoptimizer.dto.LaneDto;
import com.freightfox.laneoptimizer.dto.LaneQuoteDto;
import com.freightfox.laneoptimizer.dto.StatusMessageResponse;
import com.freightfox.laneoptimizer.dto.TransporterDto;
import com.freightfox.laneoptimizer.entity.LaneEntity;
import com.freightfox.laneoptimizer.entity.LaneQuoteEntity;
import com.freightfox.laneoptimizer.entity.TransporterEntity;
import com.freightfox.laneoptimizer.exception.InvalidInputException;
import com.freightfox.laneoptimizer.repository.LaneQuoteRepository;
import com.freightfox.laneoptimizer.repository.LaneRepository;
import com.freightfox.laneoptimizer.repository.TransporterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service managing input ingestion, cross-validation, and transactional dataset replacement.
 */
@Service
public class InputService {

    private static final Logger log = LoggerFactory.getLogger(InputService.class);

    private final LaneRepository laneRepository;
    private final TransporterRepository transporterRepository;
    private final LaneQuoteRepository laneQuoteRepository;

    public InputService(
            LaneRepository laneRepository,
            TransporterRepository transporterRepository,
            LaneQuoteRepository laneQuoteRepository
    ) {
        this.laneRepository = laneRepository;
        this.transporterRepository = transporterRepository;
        this.laneQuoteRepository = laneQuoteRepository;
    }

    /**
     * Cross-validates and replaces the entire trade lanes and transporter quotes dataset atomically.
     *
     * @param request the input payload
     * @return confirmation response
     */
    @Transactional
    public StatusMessageResponse replaceAll(InputRequest request) {
        if (request == null) {
            throw new InvalidInputException("Request payload cannot be null");
        }

        List<String> errors = new ArrayList<>();

        // Validate Lane uniqueness
        Set<Long> seenLaneIds = new HashSet<>();
        for (LaneDto lane : request.lanes()) {
            if (lane.id() == null) {
                errors.add("Lane id cannot be null");
            } else if (!seenLaneIds.add(lane.id())) {
                errors.add("Duplicate lane id found: " + lane.id());
            }
        }

        // Validate Transporter uniqueness and quote integrity
        Set<Long> seenTransporterIds = new HashSet<>();
        for (TransporterDto transporter : request.transporters()) {
            if (transporter.id() == null) {
                errors.add("Transporter id cannot be null");
            } else if (!seenTransporterIds.add(transporter.id())) {
                errors.add("Duplicate transporter id found: " + transporter.id());
            }

            if (transporter.laneQuotes() != null) {
                Set<Long> quotedLanesForThisTransporter = new HashSet<>();
                for (LaneQuoteDto quote : transporter.laneQuotes()) {
                    if (quote.laneId() != null) {
                        if (!seenLaneIds.contains(quote.laneId())) {
                            errors.add("Transporter " + transporter.id() + " references unknown lane id: " + quote.laneId());
                        }
                        if (!quotedLanesForThisTransporter.add(quote.laneId())) {
                            errors.add("Transporter " + transporter.id() + " has duplicate quote for lane id: " + quote.laneId());
                        }
                    }
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new InvalidInputException("Validation failed for input data", errors);
        }

        log.info("Ingesting new dataset: {} lanes, {} transporters", request.lanes().size(), request.transporters().size());

        // Atomically replace dataset
        laneQuoteRepository.deleteAllInBatch();
        transporterRepository.deleteAllInBatch();
        laneRepository.deleteAllInBatch();

        List<LaneEntity> laneEntities = request.lanes().stream()
                .map(l -> new LaneEntity(l.id(), l.origin().trim(), l.destination().trim()))
                .toList();
        laneRepository.saveAll(laneEntities);

        List<TransporterEntity> transporterEntities = request.transporters().stream()
                .map(t -> new TransporterEntity(t.id(), t.name().trim()))
                .toList();
        transporterRepository.saveAll(transporterEntities);

        List<LaneQuoteEntity> quoteEntities = new ArrayList<>();
        for (TransporterDto t : request.transporters()) {
            for (LaneQuoteDto q : t.laneQuotes()) {
                quoteEntities.add(new LaneQuoteEntity(t.id(), q.laneId(), q.quote()));
            }
        }
        laneQuoteRepository.saveAll(quoteEntities);

        log.info("Dataset successfully replaced: {} lanes, {} transporters, {} quotes stored",
                laneEntities.size(), transporterEntities.size(), quoteEntities.size());

        return StatusMessageResponse.success("Input data saved successfully.");
    }
}
