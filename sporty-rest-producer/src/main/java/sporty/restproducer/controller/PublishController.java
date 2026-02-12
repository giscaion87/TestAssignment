package sporty.restproducer.controller;

import java.util.concurrent.Semaphore;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import sporty.restproducer.producer.PublishProducer;

@RestController
public class PublishController implements PublishApi {
  private final Semaphore inFlight;
  private final int maxInFlight;
  private final Counter inFlightRejectCounter;
  private final PublishProducer publishProducer;

  public PublishController(
      @Value("${app.publish.max-in-flight:500}") int maxInFlight,
      MeterRegistry meterRegistry,
      PublishProducer publishProducer) {
    this.maxInFlight = Math.max(1, maxInFlight);
    this.inFlight = new Semaphore(this.maxInFlight);
    this.inFlightRejectCounter =
        Counter.builder("app.publish.inflight.rejected")
            .description("Publish requests rejected by in-flight limiter")
            .tag("status", "429")
            .register(meterRegistry);
    Gauge.builder(
            "app.publish.inflight.inuse",
            inFlight,
            semaphore -> this.maxInFlight - semaphore.availablePermits())
        .description("Current in-flight publish requests")
        .tag("max", Integer.toString(this.maxInFlight))
        .register(meterRegistry);
    Gauge.builder("app.publish.inflight.max", () -> this.maxInFlight)
        .description("Configured max in-flight publish requests")
        .register(meterRegistry);
    this.publishProducer = publishProducer;
  }

  @Override
  public PublishResponse publish(PublishRequest request, String traceId) {
    if (!inFlight.tryAcquire()) {
      inFlightRejectCounter.increment();
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests - try later");
    }
    try {
      publishProducer.publish(request, traceId);
      return new PublishResponse("accepted", request.eventId());
    } finally {
      inFlight.release();
    }
  }
}
