package sporty.restproducer.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record PublishResponse(
    @Schema(description = "Request status", example = "accepted")
        @JsonProperty("status")
        String status,
    @Schema(description = "Event identifier", example = "e1")
        @JsonProperty("event_id")
        String eventId) {}
