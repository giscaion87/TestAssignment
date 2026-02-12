package sporty.restproducer.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublishRequest(
    @NotBlank
        @Size(max = 64)
        @Schema(description = "Event identifier", example = "e1")
        @JsonProperty("event_id")
        String eventId,
    @NotBlank
        @Size(max = 128)
        @Schema(description = "Event name", example = "Final")
        @JsonProperty("event_name")
        String eventName,
    @NotBlank
        @Size(max = 64)
        @Schema(description = "Winner identifier", example = "w9")
        @JsonProperty("event_winner_id")
        String eventWinnerId) {}
