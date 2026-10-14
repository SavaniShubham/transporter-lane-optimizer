package com.laneoptimizer.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laneoptimizer.dto.AssignmentRequest;
import com.laneoptimizer.dto.InputRequest;
import com.laneoptimizer.dto.LaneDto;
import com.laneoptimizer.dto.LaneQuoteDto;
import com.laneoptimizer.dto.TransporterDto;
import com.laneoptimizer.repository.LaneQuoteRepository;
import com.laneoptimizer.repository.LaneRepository;
import com.laneoptimizer.repository.TransporterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TransporterController Integration Tests")
class TransporterControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LaneRepository laneRepository;

    @Autowired
    private TransporterRepository transporterRepository;

    @Autowired
    private LaneQuoteRepository laneQuoteRepository;

    @BeforeEach
    void cleanDatabase() {
        laneQuoteRepository.deleteAllInBatch();
        transporterRepository.deleteAllInBatch();
        laneRepository.deleteAllInBatch();
    }

    private InputRequest buildTestCase1Payload() {
        List<LaneDto> lanes = List.of(
                new LaneDto(1L, "Mumbai", "Delhi"),
                new LaneDto(2L, "Delhi", "Bangalore"),
                new LaneDto(3L, "Chennai", "Kolkata"),
                new LaneDto(4L, "Pune", "Hyderabad"),
                new LaneDto(5L, "Ahmedabad", "Jaipur")
        );

        long[][] table = {
                {20835, 48844, 39020, 14400, 11601, 35095, 26070}, // Lane 1
                {10512, 31326, 20648, 44514, 19760, 12494, 41098}, // Lane 2
                {22105, 18640, 31438, 14316, 40870, 17808, 20932}, // Lane 3
                {42481, 45828, 36447, 10678, 20635, 36210, 16897}, // Lane 4
                {19862, 18297, 12789, 13032, 26421, 39444, 27938}  // Lane 5
        };

        List<TransporterDto> transporters = new ArrayList<>();
        for (int t = 0; t < 7; t++) {
            long tId = t + 1L;
            List<LaneQuoteDto> quotes = new ArrayList<>();
            for (int l = 0; l < 5; l++) {
                quotes.add(new LaneQuoteDto((long) (l + 1), table[l][t]));
            }
            transporters.add(new TransporterDto(tId, "Transporter T" + tId, quotes));
        }

        return new InputRequest(lanes, transporters);
    }

    private InputRequest buildTestCase2Payload() {
        List<LaneDto> lanes = List.of(
                new LaneDto(1L, "Chandigarh", "Shimla"),
                new LaneDto(2L, "Agra", "Kanpur"),
                new LaneDto(3L, "Varanasi", "Gorakhpur"),
                new LaneDto(4L, "Amritsar", "Ludhiana"),
                new LaneDto(5L, "Coimbatore", "Madurai")
        );

        List<TransporterDto> transporters = List.of(
                new TransporterDto(1L, "Transporter X", List.of(
                        new LaneQuoteDto(1L, 1000L),
                        new LaneQuoteDto(2L, 1500L)
                )),
                new TransporterDto(2L, "Transporter Y", List.of(
                        new LaneQuoteDto(3L, 2000L),
                        new LaneQuoteDto(4L, 1800L)
                )),
                new TransporterDto(3L, "Transporter Z", List.of(
                        new LaneQuoteDto(5L, 1200L)
                )),
                new TransporterDto(4L, "Transporter W", List.of(
                        new LaneQuoteDto(1L, 1100L),
                        new LaneQuoteDto(4L, 1750L)
                ))
        );

        return new InputRequest(lanes, transporters);
    }

    @Test
    @DisplayName("Test Case 1 End-to-End: K=3 must return 200, totalCost 60139, selected [1, 4, 5]")
    void tc1_k3_flow() throws Exception {
        InputRequest input = buildTestCase1Payload();

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", containsString("saved successfully")));

        AssignmentRequest assignmentReq = new AssignmentRequest(3);

        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.totalCost", is(60139)))
                .andExpect(jsonPath("$.optimal", is(true)))
                .andExpect(jsonPath("$.selectedTransporters", contains(1, 4, 5)))
                .andExpect(jsonPath("$.assignments", hasSize(5)));
    }

    @Test
    @DisplayName("Test Case 1 End-to-End: K=5 must return 200, totalCost 63388, selected [1, 3, 4, 5, 6] (Interpretation B)")
    void tc1_k5_flow() throws Exception {
        InputRequest input = buildTestCase1Payload();

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk());

        AssignmentRequest assignmentReq = new AssignmentRequest(5);

        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.totalCost", is(63388)))
                .andExpect(jsonPath("$.optimal", is(true)))
                .andExpect(jsonPath("$.selectedTransporters", contains(1, 3, 4, 5, 6)));
    }

    @Test
    @DisplayName("Test Case 2 End-to-End: K=3 must return 7500, K=4 must return 7450")
    void tc2_flow() throws Exception {
        InputRequest input = buildTestCase2Payload();

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk());

        // K=3 -> 7500
        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignmentRequest(3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost", is(7500)))
                .andExpect(jsonPath("$.selectedTransporters", contains(1, 2, 3)));

        // K=4 -> 7450
        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignmentRequest(4))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost", is(7450)))
                .andExpect(jsonPath("$.selectedTransporters", contains(1, 2, 3, 4)));
    }

    @Test
    @DisplayName("Test Case 2: K=2 is proven infeasible and must return HTTP 422")
    void tc2_k2_infeasible() throws Exception {
        InputRequest input = buildTestCase2Payload();

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignmentRequest(2))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("infeasible")));
    }

    @Test
    @DisplayName("Assignment before input must return HTTP 404")
    void assignment_before_input_404() throws Exception {
        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignmentRequest(3))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    @DisplayName("Validation failure: negative quote must return HTTP 400")
    void negative_quote_400() throws Exception {
        String badPayload = """
                {
                  "lanes": [{"id": 1, "origin": "Mumbai", "destination": "Delhi"}],
                  "transporters": [
                    {
                      "id": 1,
                      "name": "T1",
                      "laneQuotes": [{"laneId": 1, "quote": -500}]
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.details", not(empty())));
    }

    @Test
    @DisplayName("Validation failure: malformed JSON body must return HTTP 400")
    void bad_json_400() throws Exception {
        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"badJson\": [unclosed"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    @DisplayName("Re-posting input atomically replaces previous dataset")
    void repost_replaces_data() throws Exception {
        InputRequest input1 = buildTestCase1Payload();
        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input1)))
                .andExpect(status().isOk());

        // Now post a simple single-lane dataset
        InputRequest singleLane = new InputRequest(
                List.of(new LaneDto(99L, "Goa", "Pune")),
                List.of(new TransporterDto(88L, "T-Single", List.of(new LaneQuoteDto(99L, 4200L))))
        );

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(singleLane)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignmentRequest(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost", is(4200)))
                .andExpect(jsonPath("$.selectedTransporters", contains(88)))
                .andExpect(jsonPath("$.assignments[0].laneId", is(99)));
    }

    @Test
    @DisplayName("Validation failure: missing or empty required fields must return HTTP 400")
    void missing_fields_400() throws Exception {
        String missingLanesPayload = """
                {
                  "lanes": [],
                  "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": 100}]}]
                }
                """;

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(missingLanesPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.details", not(empty())));
    }

    @Test
    @DisplayName("Validation failure: duplicate lane IDs in input must return HTTP 400")
    void duplicate_ids_400() throws Exception {
        String duplicateLanePayload = """
                {
                  "lanes": [
                    {"id": 1, "origin": "Mumbai", "destination": "Delhi"},
                    {"id": 1, "origin": "Pune", "destination": "Bangalore"}
                  ],
                  "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 1, "quote": 100}]}]
                }
                """;

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateLanePayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.details[0]", containsString("Duplicate lane id found")));
    }

    @Test
    @DisplayName("Validation failure: quote referencing unknown lane ID must return HTTP 400")
    void unknown_lane_in_quote_400() throws Exception {
        String unknownLanePayload = """
                {
                  "lanes": [{"id": 1, "origin": "Mumbai", "destination": "Delhi"}],
                  "transporters": [{"id": 1, "name": "T1", "laneQuotes": [{"laneId": 999, "quote": 100}]}]
                }
                """;

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unknownLanePayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.details[0]", containsString("references unknown lane id: 999")));
    }
}
