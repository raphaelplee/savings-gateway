package hello;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@SpringBootApplication
@EnableConfigurationProperties(Application.UriConfiguration.class)
@RestController
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder, UriConfiguration uriConfiguration) {
        String httphost = uriConfiguration.getHttpHost();
        return builder.routes()
                .route(r -> r.path("/savings/a/**")
                        .filters(f -> f
                                .rewritePath("/savings/a/", "/")
                                .circuitBreaker(config -> config.setName("savingsA"))
                        )
                        .uri(httphost + ":8081/"))

                .route(r -> r.path("/savings/b/**")
                        .filters(f -> f
                                .rewritePath("/savings/b/", "/")
                                .circuitBreaker(config -> config.setName("savingsB"))
                        )
                        .uri(httphost + ":8082/"))
                .build();
    }

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
                .timeLimiterConfig(TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(5000)).build())
                .build());
    }


    @ConfigurationProperties
    class UriConfiguration {

        @Value("${service.savings.hostname:http://192.168.0.102}")
        private String httphost = "http://192.168.0.102";

        public String getHttpHost() {
            return httphost;
        }

        public void setHttpHost(String httpbin) {
            this.httphost = httpbin;
        }
    }
}

