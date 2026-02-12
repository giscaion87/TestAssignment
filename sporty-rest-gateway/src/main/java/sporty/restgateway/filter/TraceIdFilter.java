package sporty.restgateway.filter;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TraceIdFilter implements GlobalFilter, Ordered {
  private static final String TRACE_HEADER = "X-Request-Trace-Id";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ThreadLocalRandom random = ThreadLocalRandom.current();
    String traceId = new UUID(random.nextLong(), random.nextLong()).toString();
    ServerHttpRequest mutatedRequest = exchange.getRequest().mutate().header(TRACE_HEADER, traceId).build();
    return chain.filter(exchange.mutate().request(mutatedRequest).build());
  }

  @Override
  public int getOrder() {
    return -1;
  }
}
