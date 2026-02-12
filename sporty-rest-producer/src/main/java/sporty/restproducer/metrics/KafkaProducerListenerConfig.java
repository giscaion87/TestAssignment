package sporty.restproducer.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.nio.ByteBuffer;
import java.time.Duration;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.ProducerListener;

@Configuration
public class KafkaProducerListenerConfig {
  public static final String SEND_START_NANOS_HEADER = "x-send-start-nanos";

  @Bean
  public ProducerListener<Object, Object> producerListener(MeterRegistry meterRegistry) {
    return new ProducerListener<>() {
      @Override
      public void onSuccess(ProducerRecord<Object, Object> record, RecordMetadata metadata) {
        recordLatency(record, meterRegistry);
        Counter.builder("app.kafka.producer.send.success")
            .description("Kafka producer send successes")
            .tag("topic", record.topic())
            .register(meterRegistry)
            .increment();
      }

      @Override
      public void onError(ProducerRecord<Object, Object> record, RecordMetadata metadata, Exception exception) {
        recordLatency(record, meterRegistry);
        Counter.builder("app.kafka.producer.send.error")
            .description("Kafka producer send errors")
            .tag("topic", record.topic())
            .register(meterRegistry)
            .increment();
      }

      private void recordLatency(ProducerRecord<Object, Object> record, MeterRegistry meterRegistry) {
        Header header = record.headers().lastHeader(SEND_START_NANOS_HEADER);
        if (header == null) {
          return;
        }
        byte[] value = header.value();
        if (value == null || value.length != Long.BYTES) {
          return;
        }
        long startNanos = ByteBuffer.wrap(value).getLong();
        long elapsedNanos = System.nanoTime() - startNanos;
        if (elapsedNanos < 0) {
          return;
        }
        Timer.builder("app.kafka.producer.send.latency")
            .description("Kafka producer send latency")
            .tag("topic", record.topic())
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram(true)
            .register(meterRegistry)
            .record(Duration.ofNanos(elapsedNanos));
      }
    };
  }
}
