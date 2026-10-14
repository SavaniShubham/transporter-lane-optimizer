package com.freightfox.laneoptimizer.web;

import com.freightfox.laneoptimizer.dto.AssignmentRequest;
import com.freightfox.laneoptimizer.dto.AssignmentResponse;
import com.freightfox.laneoptimizer.dto.ErrorResponse;
import com.freightfox.laneoptimizer.dto.InputRequest;
import com.freightfox.laneoptimizer.dto.StatusMessageResponse;
import com.freightfox.laneoptimizer.service.AssignmentService;
import com.freightfox.laneoptimizer.service.InputService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing endpoints for quote ingestion and lane assignment optimization.
 */
@RestController
@RequestMapping("/api/v1/transporters")
@Tag(name = "Transporters & Lanes Optimization API", description = "Endpoints for managing trade lanes, quotes, and optimal transporter assignments.")
public class TransporterController {

    private final InputService inputService;
    private final AssignmentService assignmentService;

    public TransporterController(InputService inputService, AssignmentService assignmentService) {
        this.inputService = inputService;
        this.assignmentService = assignmentService;
    }

    @PostMapping("/input")
    @Operation(
            summary = "Save or replace lane and transporter quote dataset",
            description = "Validates and atomically replaces all active trade lanes, transporters, and quote data."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dataset successfully validated and saved",
                    content = @Content(schema = @Schema(implementation = StatusMessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed on input payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<StatusMessageResponse> saveInput(@Valid @RequestBody InputRequest request) {
        StatusMessageResponse response = inputService.replaceAll(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/assignment")
    @Operation(
            summary = "Compute lane assignments subject to maximum transporter constraint",
            description = "Assigns trade lanes to transporters using a lexicographic objective: " +
                    "(1) maximize the number of distinct transporters actually assigned lanes up to maxTransporters (K), and " +
                    "(2) minimize total procurement cost among maximal-usage assignments. " +
                    "The response contains an 'optimal' boolean indicating whether the result was proven mathematically optimal or generated via heuristic search."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Assignment computed successfully (check 'optimal' field for mathematical guarantee)",
                    content = @Content(schema = @Schema(implementation = AssignmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request constraint",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No input dataset has been uploaded yet",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Assignment is proven infeasible (e.g. orphan lanes or mandatory transporters > K)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Search budget exceeded without determining feasibility",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AssignmentResponse> computeAssignment(@Valid @RequestBody AssignmentRequest request) {
        AssignmentResponse response = assignmentService.calculateAssignment(request);
        return ResponseEntity.ok(response);
    }
}
