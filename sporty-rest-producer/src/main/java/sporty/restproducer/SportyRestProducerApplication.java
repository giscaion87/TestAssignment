package sporty.restproducer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(
    info =
        @Info(
            title = "Sporty API",
            version = "v1",
            description = "REST API for publishing events"))
public class SportyRestProducerApplication {
  static void main(String[] args) {
    SpringApplication.run(SportyRestProducerApplication.class, args);
  }
}
