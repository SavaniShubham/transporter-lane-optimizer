package com.freightfox.laneoptimizer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Data transfer object representing a transporter input with quotes.
 */
public record TransporterDto(
        @Schema(description = "Unique identifier of the transporter", example = "1")
        @NotNull(message = "Transporter id cannot be null")
        Long id,

        @Schema(description = "Name of the transporter", example = "Transporter T1")
        @NotBlank(message = "Transporter name cannot be blank")
        String name,

        @Schema(description = "List of quotes provided by this transporter")
        @NotEmpty(message = "Transporter must provide at least one lane quote")
        @Valid
        List<LaneQuoteDto> laneQuotes
) {}
