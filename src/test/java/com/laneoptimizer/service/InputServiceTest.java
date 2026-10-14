package com.laneoptimizer.service;

import com.laneoptimizer.dto.InputRequest;
import com.laneoptimizer.dto.LaneDto;
import com.laneoptimizer.dto.LaneQuoteDto;
import com.laneoptimizer.dto.StatusMessageResponse;
import com.laneoptimizer.dto.TransporterDto;
import com.laneoptimizer.exception.InvalidInputException;
import com.laneoptimizer.repository.LaneQuoteRepository;
import com.laneoptimizer.repository.LaneRepository;
import com.laneoptimizer.repository.TransporterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("InputService Unit Tests")
class InputServiceTest {

    @Mock
    private LaneRepository laneRepository;

    @Mock
    private TransporterRepository transporterRepository;

    @Mock
    private LaneQuoteRepository laneQuoteRepository;

    private InputService inputService;

    @BeforeEach
    void setUp() {
        inputService = new InputService(laneRepository, transporterRepository, laneQuoteRepository);
    }

    @Test
    @DisplayName("Should reject input with duplicate lane IDs")
    void duplicate_lane_ids() {
        InputRequest request = new InputRequest(
                List.of(
                        new LaneDto(1L, "Mumbai", "Delhi"),
                        new LaneDto(1L, "Pune", "Bangalore") // Duplicate id=1
                ),
                List.of(
                        new TransporterDto(1L, "T1", List.of(new LaneQuoteDto(1L, 100L)))
                )
        );

        InvalidInputException ex = assertThrows(InvalidInputException.class, () -> inputService.replaceAll(request));
        assertTrue(ex.getDetails().stream().anyMatch(d -> d.contains("Duplicate lane id found: 1")));
    }

    @Test
    @DisplayName("Should reject input with duplicate transporter IDs")
    void duplicate_transporter_ids() {
        InputRequest request = new InputRequest(
                List.of(new LaneDto(1L, "Mumbai", "Delhi")),
                List.of(
                        new TransporterDto(1L, "T1", List.of(new LaneQuoteDto(1L, 100L))),
                        new TransporterDto(1L, "T1-bis", List.of(new LaneQuoteDto(1L, 200L))) // Duplicate id=1
                )
        );

        InvalidInputException ex = assertThrows(InvalidInputException.class, () -> inputService.replaceAll(request));
        assertTrue(ex.getDetails().stream().anyMatch(d -> d.contains("Duplicate transporter id found: 1")));
    }

    @Test
    @DisplayName("Should reject input where a quote references an unknown lane ID")
    void unknown_lane_in_quote() {
        InputRequest request = new InputRequest(
                List.of(new LaneDto(1L, "Mumbai", "Delhi")),
                List.of(
                        new TransporterDto(1L, "T1", List.of(new LaneQuoteDto(99L, 100L))) // laneId 99 does not exist
                )
        );

        InvalidInputException ex = assertThrows(InvalidInputException.class, () -> inputService.replaceAll(request));
        assertTrue(ex.getDetails().stream().anyMatch(d -> d.contains("references unknown lane id: 99")));
    }

    @Test
    @DisplayName("Should reject input where a transporter submits duplicate quotes for the same lane")
    void duplicate_quote_per_transporter() {
        InputRequest request = new InputRequest(
                List.of(new LaneDto(1L, "Mumbai", "Delhi")),
                List.of(
                        new TransporterDto(1L, "T1", List.of(
                                new LaneQuoteDto(1L, 100L),
                                new LaneQuoteDto(1L, 120L) // Duplicate quote for lane 1
                        ))
                )
        );

        InvalidInputException ex = assertThrows(InvalidInputException.class, () -> inputService.replaceAll(request));
        assertTrue(ex.getDetails().stream().anyMatch(d -> d.contains("has duplicate quote for lane id: 1")));
    }

    @Test
    @DisplayName("Should successfully replace previous dataset on valid input")
    void successful_save() {
        InputRequest request = new InputRequest(
                List.of(new LaneDto(1L, "Mumbai", "Delhi")),
                List.of(new TransporterDto(1L, "T1", List.of(new LaneQuoteDto(1L, 100L))))
        );

        StatusMessageResponse response = inputService.replaceAll(request);

        assertEquals("success", response.status());
        assertEquals("Input data saved successfully.", response.message());

        verify(laneQuoteRepository).deleteAllInBatch();
        verify(transporterRepository).deleteAllInBatch();
        verify(laneRepository).deleteAllInBatch();

        verify(laneRepository).saveAll(anyList());
        verify(transporterRepository).saveAll(anyList());
        verify(laneQuoteRepository).saveAll(anyList());
    }
}
