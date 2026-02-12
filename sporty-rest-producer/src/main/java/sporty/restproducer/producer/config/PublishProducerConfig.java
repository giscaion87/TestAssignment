package sporty.restproducer.producer.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class PublishProducerConfig {
    private final String topicName;
    private final int partitions;
    private final short replicationFactor;
    private final String cleanupPolicy;
    private final long retentionMs;
    private final long retentionBytes;
    private final int segmentBytes;
    private final int minInSyncReplicas;

    public PublishProducerConfig(
            @Value("${app.kafka.topic.name}") String topicName,
            @Value("${app.kafka.topic.partitions}") int partitions,
            @Value("${app.kafka.topic.replication-factor}") short replicationFactor,
            @Value("${app.kafka.topic.cleanup-policy}") String cleanupPolicy,
            @Value("${app.kafka.topic.retention-ms}") long retentionMs,
            @Value("${app.kafka.topic.retention-bytes}") long retentionBytes,
            @Value("${app.kafka.topic.segment-bytes}") int segmentBytes,
            @Value("${app.kafka.topic.min-in-sync-replicas}") int minInSyncReplicas) {
        this.topicName = topicName;
        this.partitions = partitions;
        this.replicationFactor = replicationFactor;
        this.cleanupPolicy = cleanupPolicy;
        this.retentionMs = retentionMs;
        this.retentionBytes = retentionBytes;
        this.segmentBytes = segmentBytes;
        this.minInSyncReplicas = minInSyncReplicas;
    }

    @Bean
    public NewTopic publishTopic() {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicationFactor)
                .config("cleanup.policy", cleanupPolicy)
                .config("min.insync.replicas", Integer.toString(minInSyncReplicas))
                .config("retention.ms", Long.toString(retentionMs))
                .config("retention.bytes", Long.toString(retentionBytes))
                .config("segment.bytes", Integer.toString(segmentBytes))
                .build();
    }
}
