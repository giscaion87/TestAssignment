package sporty.restproducer.producer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import sporty.restproducer.controller.PublishRequest;
import sporty.restproducer.metrics.KafkaProducerListenerConfig;
import sporty.restproducer.proto.Event;

@Service
public class PublishProducer {
    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final String topic;

    public PublishProducer(
            KafkaTemplate<String, byte[]> kafkaTemplate,
            @Value("${app.kafka.topic.name:event-outcomes}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(PublishRequest request, String traceId) {
        Event event = Event.newBuilder()
                .setEventId(request.eventId())
                .setEventName(request.eventName())
                .setEventWinnerId(request.eventWinnerId())
                .build();

        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, request.eventId(), event.toByteArray());
        record.headers()
                .add(new RecordHeader(
                        KafkaProducerListenerConfig.SEND_START_NANOS_HEADER,
                        ByteBuffer.allocate(Long.BYTES).putLong(System.nanoTime()).array()))
                .add(new RecordHeader("request_trace_id", traceId.getBytes(StandardCharsets.UTF_8)));

        kafkaTemplate.send(record);
    }
}
