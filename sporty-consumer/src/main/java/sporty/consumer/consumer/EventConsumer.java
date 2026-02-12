package sporty.consumer.consumer;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.retry.annotation.Backoff;
import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.stereotype.Service;
import sporty.consumer.producer.BetSettlementPublisher;
import sporty.restproducer.proto.Event;

@Service
public class EventConsumer {
    private static final String TRACE_HEADER = "request_trace_id";
    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);
    private final BetSettlementPublisher betSettlementPublisher;

    public EventConsumer(BetSettlementPublisher betSettlementPublisher) {
        this.betSettlementPublisher = betSettlementPublisher;
    }

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 10.0, maxDelay = 20000),
            exclude = {InvalidProtocolBufferException.class},
            retryTopicSuffix = "_retry_",
            dltTopicSuffix = "_dlt",
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    @KafkaListener(topics = "${app.kafka.topic}")
    public void consume(ConsumerRecord<String, byte[]> record, Acknowledgment acknowledgment) throws InvalidProtocolBufferException {
        String traceId = null;
        var header = record.headers().lastHeader(TRACE_HEADER);
        if (header != null) {
            traceId = new String(header.value(), StandardCharsets.UTF_8);
        }

        try {
            Event event = Event.parseFrom(record.value());
            betSettlementPublisher.publishForEvent(event.getEventId(), event.getEventWinnerId());
            log.info(
                    "event received topic={} partition={} offset={} traceId={} eventId={} eventName={} winnerId={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    traceId,
                    event.getEventId(),
                    event.getEventName(),
                    event.getEventWinnerId());
            acknowledgment.acknowledge();
        } catch (Exception ex) {
            log.error(
                    "failed to decode event topic={} partition={} offset={} key={} traceId={} error={}",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    traceId,
                    ex.toString());
            throw ex;
        }
    }
}
