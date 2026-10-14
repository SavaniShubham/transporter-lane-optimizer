package com.freightfox.laneoptimizer.service;

import com.freightfox.laneoptimizer.domain.LaneAssignment;
import com.freightfox.laneoptimizer.domain.OptimizationResult;
import com.freightfox.laneoptimizer.dto.AssignmentRequest;
import com.freightfox.laneoptimizer.dto.AssignmentResponse;
import com.freightfox.laneoptimizer.entity.LaneEntity;
import com.freightfox.laneoptimizer.entity.LaneQuoteEntity;
import com.freightfox.laneoptimizer.entity.TransporterEntity;
import com.freightfox.laneoptimizer.exception.NoInputDataException;
import com.freightfox.laneoptimizer.optimizer.LaneOptimizer;
import com.freightfox.laneoptimizer.optimizer.QuoteMatrix;
import com.freightfox.laneoptimizer.repository.LaneQuoteRepository;
import com.freightfox.laneoptimizer.repository.LaneRepository;
import com.freightfox.laneoptimizer.repository.TransporterRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentService Unit Tests")
class AssignmentServiceTest {

    @Mock
    private LaneRepository laneRepository;

    @Mock
    private TransporterRepository transporterRepository;

    @Mock
    private LaneQuoteRepository laneQuoteRepository;

    @Test
    @DisplayName("Should throw NoInputDataException when database has no saved input")
    void no_data_throws_404() {
        when(laneRepository.findAll()).thenReturn(List.of());

        AssignmentService service = new AssignmentService(
                laneRepository,
                transporterRepository,
                laneQuoteRepository,
                new LaneOptimizer()
        );

        assertThrows(NoInputDataException.class, () -> service.calculateAssignment(new AssignmentRequest(3)));
    }

    @Test
    @DisplayName("Should successfully calculate assignment and map to response")
    void successful_assignment_mapping() {
        when(laneRepository.findAll()).thenReturn(List.of(new LaneEntity(1L, "Mumbai", "Delhi")));
        when(transporterRepository.findAll()).thenReturn(List.of(new TransporterEntity(1L, "T1")));
        when(laneQuoteRepository.findAll()).thenReturn(List.of(new LaneQuoteEntity(1L, 1L, 5000L)));

        OptimizationResult optResult = new OptimizationResult(
                5000L,
                List.of(new LaneAssignment(1L, 1L, 5000L)),
                List.of(1L)
        );

        LaneOptimizer stubOptimizer = new LaneOptimizer() {
            @Override
            public FinalResult optimize(QuoteMatrix matrix, int maxTransporters) {
                return new FinalResult(optResult, true);
            }
        };

        AssignmentService service = new AssignmentService(
                laneRepository,
                transporterRepository,
                laneQuoteRepository,
                stubOptimizer
        );

        AssignmentResponse response = service.calculateAssignment(new AssignmentRequest(1));

        assertEquals("success", response.status());
        assertEquals(5000L, response.totalCost());
        assertTrue(response.optimal());
        assertEquals(1, response.assignments().size());
        assertEquals(List.of(1L), response.selectedTransporters());
    }
}
