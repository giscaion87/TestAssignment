package sporty.restproducer.filter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

@Component
public class TraceIdFilter extends HttpFilter {
  private static final String TRACE_HEADER = "X-Request-Trace-Id";

  @Override
  protected void doFilter(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    String traceId = request.getHeader(TRACE_HEADER);
    if (traceId != null && !traceId.isBlank()) {
      chain.doFilter(request, response);
      return;
    }

    ThreadLocalRandom random = ThreadLocalRandom.current();
    String generated = new UUID(random.nextLong(), random.nextLong()).toString();
    HttpServletRequest wrapped =
        new HttpServletRequestWrapper(request) {
          @Override
          public String getHeader(String name) {
            if (TRACE_HEADER.equalsIgnoreCase(name)) {
              return generated;
            }
            return super.getHeader(name);
          }
        };
    chain.doFilter(wrapped, response);
  }
}
