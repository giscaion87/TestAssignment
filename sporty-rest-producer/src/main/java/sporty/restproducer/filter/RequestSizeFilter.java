package sporty.restproducer.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestSizeFilter extends OncePerRequestFilter {
  private static final String TOO_LARGE_MESSAGE = "Request payload too large";

  private final long maxBytes;

  public RequestSizeFilter(@Value("${app.publish.max-request-bytes:2048}") long maxBytes) {
    this.maxBytes = Math.max(1, maxBytes);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (!isPublishRequest(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    long contentLength = request.getContentLengthLong();
    if (contentLength > maxBytes) {
      response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, TOO_LARGE_MESSAGE);
      return;
    }

    HttpServletRequest wrapped = request;
    if (contentLength < 0) {
      wrapped = new LimitedRequestWrapper(request, maxBytes);
    }

    try {
      filterChain.doFilter(wrapped, response);
    } catch (IOException ex) {
      if (isTooLargeException(ex) && !response.isCommitted()) {
        response.sendError(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, TOO_LARGE_MESSAGE);
        return;
      }
      throw ex;
    }
  }

  private boolean isPublishRequest(HttpServletRequest request) {
    return "POST".equalsIgnoreCase(request.getMethod()) && "/publish".equals(request.getRequestURI());
  }

  private boolean isTooLargeException(IOException ex) {
    String message = ex.getMessage();
    return message != null && message.contains(TOO_LARGE_MESSAGE);
  }

  private static final class LimitedRequestWrapper extends HttpServletRequestWrapper {
    private final long maxBytes;

    private LimitedRequestWrapper(HttpServletRequest request, long maxBytes) {
      super(request);
      this.maxBytes = maxBytes;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
      return new LimitedServletInputStream(super.getInputStream(), maxBytes);
    }

    @Override
    public BufferedReader getReader() throws IOException {
      Charset charset = Charset.forName(getCharacterEncoding() == null ? "UTF-8" : getCharacterEncoding());
      return new BufferedReader(new InputStreamReader(getInputStream(), charset));
    }
  }

  private static final class LimitedServletInputStream extends ServletInputStream {
    private final ServletInputStream delegate;
    private final long maxBytes;
    private long count;

    private LimitedServletInputStream(ServletInputStream delegate, long maxBytes) {
      this.delegate = delegate;
      this.maxBytes = maxBytes;
    }

    @Override
    public int read() throws IOException {
      int value = delegate.read();
      if (value != -1) {
        count++;
        if (count > maxBytes) {
          throw new IOException(TOO_LARGE_MESSAGE);
        }
      }
      return value;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      int read = delegate.read(b, off, len);
      if (read > 0) {
        count += read;
        if (count > maxBytes) {
          throw new IOException(TOO_LARGE_MESSAGE);
        }
      }
      return read;
    }

    @Override
    public boolean isFinished() {
      return delegate.isFinished();
    }

    @Override
    public boolean isReady() {
      return delegate.isReady();
    }

    @Override
    public void setReadListener(jakarta.servlet.ReadListener readListener) {
      delegate.setReadListener(readListener);
    }
  }
}
