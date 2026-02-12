package sporty.consumer.config;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RocketMqProducerConfig {
  @Bean(destroyMethod = "shutdown")
  public DefaultMQProducer rocketMQProducer(
      @Value("${rocketmq.name-server}") String nameServer,
      @Value("${rocketmq.producer.group}") String producerGroup,
      @Value("${rocketmq.producer.send-message-timeout:10000}") int sendTimeoutMs,
      @Value("${rocketmq.producer.retry-times-when-send-async-failed:2}") int asyncRetryTimes) {
    DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
    producer.setNamesrvAddr(nameServer);
    producer.setSendMsgTimeout(sendTimeoutMs);
    producer.setRetryTimesWhenSendAsyncFailed(asyncRetryTimes);
    return producer;
  }

  @Bean
  public RocketMQTemplate rocketMQTemplate(DefaultMQProducer rocketMQProducer) {
    RocketMQTemplate template = new RocketMQTemplate();
    template.setProducer(rocketMQProducer);
    return template;
  }
}
