package sporty.consumer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RocketMqConfigLogger {
  private static final Logger log = LoggerFactory.getLogger(RocketMqConfigLogger.class);
  private final String nameServer;

  public RocketMqConfigLogger(@Value("${rocketmq.name-server:}") String nameServer) {
    this.nameServer = nameServer;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void logRocketMqConfig() {
    if (nameServer == null || nameServer.isBlank()) {
      log.warn("rocketmq.name-server is empty");
      return;
    }
    log.info("rocketmq.name-server={}", nameServer);
  }
}
