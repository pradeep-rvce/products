package com.handson.productcompositeservice;

import com.handson.productcompositeservice.service.ProductCompositeIntegration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebFlux;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import static springfox.documentation.builders.RequestHandlerSelectors.basePackage;
import static springfox.documentation.spi.DocumentationType.SWAGGER_2;

@SpringBootApplication
@EnableSwagger2WebFlux
@ComponentScan({"com.handson.api.cpre", "com.handson.util", "com.handson.productcompositeservice"})
public class ProductCompositeServiceApplication {

    @Value("${api.common.version}")
    String apiVersion;
    @Value("${api.common.title}")
    String apiTitle;
    @Value("${api.common.description}")
    String apiDescription;
    @Value("${api.common.termsOfServiceUrl}")
    String apiTermsOfServiceUrl;
    @Value("${api.common.license}")
    String apiLicense;
    @Value("${api.common.licenseUrl}")
    String apiLicenseUrl;
    @Value("${api.common.contact.name}")
    String apiContactName;
    @Value("${api.common.contact.url}")
    String apiContactUrl;
    @Value("${api.common.contact.email}")
    String apiContactEmail;

    @Autowired
    StatusAggregator healthAggregator;
    @Autowired
    ProductCompositeIntegration integration;
    @Autowired
    RecommendationHealthIndicator recommendationHealthIndicator;
    @Autowired
    ReviewHealthIndicator reviewHealthIndicator;
    @Autowired
    ProductHealthIndicator productHealthIndicator;

    public static void main(String[] args) {
        SpringApplication.run(ProductCompositeServiceApplication.class, args);
    }

    /**
     * Will exposed on $HOST:$PORT/swagger-ui.html
     *
     * @return
     */
    @Bean
    public Docket apiDocumentation() {

        return new Docket(SWAGGER_2)
                .select()
                .apis(basePackage("se.magnus.microservices.composite.product"))
                .paths(PathSelectors.any())
                .build()
                .globalResponseMessage(POST, emptyList())
                .globalResponseMessage(GET, emptyList())
                .globalResponseMessage(DELETE, emptyList())
                .apiInfo(new ApiInfo(
                        apiTitle,
                        apiDescription,
                        apiVersion,
                        apiTermsOfServiceUrl,
                        new Contact(apiContactName, apiContactUrl, apiContactEmail),
                        apiLicense,
                        apiLicenseUrl,
                        emptyList()
                ));
    }

    @Bean
    ReactiveHealthContributor coreServices() {
        Map<String, ReactiveHealthIndicator> registry = new HashMap<>();
        registry.put("product", productHealthIndicator);
        registry.put("recommendation", recommendationHealthIndicator);
        registry.put("review", reviewHealthIndicator);
        return CompositeReactiveHealthContributor.fromMap(registry);
    }

    @Component
    class ProductHealthIndicator implements ReactiveHealthIndicator {
        @Override
        public Mono<Health> health() {
            return integration.getProductHealth();
        }
    }

    @Component
    class RecommendationHealthIndicator implements ReactiveHealthIndicator {
        @Override
        public Mono<Health> health() {
            return integration.getRecommendationHealth();
        }
    }

    @Component
    class ReviewHealthIndicator implements ReactiveHealthIndicator {
        @Override
        public Mono<Health> health() {
            return integration.getReviewHealth();
        }
    }

    @Bean("eur")
    @Primary
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        final WebClient.Builder builder = WebClient.builder();
        return builder;
    }
}