package sporty.restproducer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;

public interface PublishApi {
  String TRACE_HEADER = "X-Request-Trace-Id";

  @PostMapping(path = "/publish")
  @ResponseStatus(HttpStatus.ACCEPTED)
  @Operation(
      summary = "Publish an event",
      description = "Accepts an event payload and returns an acceptance response.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "202",
        description = "Accepted",
        content = @Content(schema = @Schema(implementation = PublishResponse.class))),
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content),
    @ApiResponse(responseCode = "413", description = "Payload too large", content = @Content),
    @ApiResponse(responseCode = "429", description = "Too many in-flight requests", content = @Content)
  })
  PublishResponse publish(
      @Valid @RequestBody PublishRequest request,
      @Parameter(
              name = TRACE_HEADER,
              in = ParameterIn.HEADER,
              description = "Internal trace id for downstream correlation")
          @RequestHeader(value = TRACE_HEADER, required = false)
          String traceId);
}
