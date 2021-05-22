package com.handson.productedge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class HealthCheckConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(HealthCheckConfiguration.class);
    private final WebClient webClient;

    @Autowired
    public HealthCheckConfiguration(
            WebClient.Builder webClientBuilder
    ) {
        webClient = webClientBuilder.build();
    }

    private static Health apply(String s) {
        return new Health.Builder().up().build();
    }

    @Bean
    ReactiveHealthContributor coreServices() {
        Map<String, ReactiveHealthIndicator> registry = new HashMap<>();
        registry.put("product", () -> getHealth("http://product"));
        registry.put("recommendation", () -> getHealth("http://recommendation"));
        registry.put("review", () -> getHealth("http://review"));
        registry.put("productComposite", () -> getHealth("http://product-composite"));
        return CompositeReactiveHealthContributor.fromMap(registry);
    }


    private Mono<Health> getHealth(String url) {
        url += "/actuator/health";
        LOG.debug("Will call the Health API on URL: {}", url);
        return webClient.get().uri(url).retrieve().bodyToMono(String.class)
                .map(HealthCheckConfiguration::apply)
                .onErrorResume(ex -> Mono.just(new Health.Builder().down(ex).build()))
                .log();
    }
}