package sporty.restproducer.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ProducerFactory;

@Configuration
public class KafkaMetricsConfig {
  @Bean
  public KafkaClientMetrics kafkaClientMetrics(
      ProducerFactory<?, ?> producerFactory, MeterRegistry meterRegistry) {
    Producer<?, ?> producer = producerFactory.createProducer();
    KafkaClientMetrics metrics = new KafkaClientMetrics(producer);
    metrics.bindTo(meterRegistry);
    return metrics;
  }
}
