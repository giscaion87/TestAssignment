package sporty.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SportyConsumerApplication {
  static void main(String[] args) {
    SpringApplication.run(SportyConsumerApplication.class, args);
  }
}
