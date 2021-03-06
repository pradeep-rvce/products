package com.handson.productcompositeservice;

import com.handson.api.cpre.composite.ProductAggregate;
import com.handson.api.cpre.composite.RecommendationSummary;
import com.handson.api.cpre.composite.ReviewSummary;
import com.handson.api.cpre.product.Product;
import com.handson.api.cpre.recommendation.Recommendation;
import com.handson.api.cpre.review.Review;
import com.handson.productcompositeservice.service.ProductCompositeIntegration;
import com.handson.util.exception.InvalidInputException;
import com.handson.util.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;

@SpringBootTest(
        webEnvironment=RANDOM_PORT,
        classes = {ProductCompositeServiceApplication.class, TestConfig.class},
        properties = {"spring.data.mongodb.port: 0","spring.cloud.config.enabled=false", "server.error.include-message=always", "spring.main.allow-bean-definition-overriding=true"})
@ActiveProfiles("test")
public class ProductCompositeServiceApplicationTests {


    private static final int PRODUCT_ID_OK = 1;
    private static final int PRODUCT_ID_NOT_FOUND = 2;
    private static final int PRODUCT_ID_INVALID = 3;

    @MockBean
    private ProductCompositeIntegration compositeIntegration;

    @MockBean
    StatusAggregator healthAggregator;


    @BeforeEach
    public void setUp() {

        when(compositeIntegration.getProduct(PRODUCT_ID_OK, 0, 0)).
                thenReturn(Mono.just(new Product(PRODUCT_ID_OK, "name", 1, "mock-address")));
        when(compositeIntegration.getRecommendations(PRODUCT_ID_OK)).
                thenReturn(Flux.just(new Recommendation(PRODUCT_ID_OK, 1, "author", 1, "content", "mock address")));
        when(compositeIntegration.getReviews(PRODUCT_ID_OK)).
                thenReturn(Flux.just(new Review(PRODUCT_ID_OK, 1, "author", "subject", "content", "mock address")));

        when(compositeIntegration.getProduct(PRODUCT_ID_NOT_FOUND, 0, 0)).thenThrow(new NotFoundException("NOT FOUND: " + PRODUCT_ID_NOT_FOUND));

        when(compositeIntegration.getProduct(PRODUCT_ID_INVALID, 0, 0)).thenThrow(new InvalidInputException("INVALID: " + PRODUCT_ID_INVALID));
    }

    @Test
    public void contextLoads() {
    }

    @Test
    public void createCompositeProduct1(@Autowired WebTestClient client) {

        ProductAggregate compositeProduct = new ProductAggregate(1, "name", 1, null, null, null);

        postAndVerifyProduct(client, compositeProduct, OK);
    }

    @Test
    public void createCompositeProduct2(@Autowired WebTestClient client) {
        ProductAggregate compositeProduct = new ProductAggregate(1, "name", 1,
                singletonList(new RecommendationSummary(1, "a", 1, "c")),
                singletonList(new ReviewSummary(1, "a", "s", "c")), null);

        postAndVerifyProduct(client, compositeProduct, OK);
    }

    @Test
    public void deleteCompositeProduct(@Autowired WebTestClient client) {
        ProductAggregate compositeProduct = new ProductAggregate(1, "name", 1,
                singletonList(new RecommendationSummary(1, "a", 1, "c")),
                singletonList(new ReviewSummary(1, "a", "s", "c")), null);

        postAndVerifyProduct(client, compositeProduct, OK);

        deleteAndVerifyProduct(client, compositeProduct.getProductId(), OK);
        deleteAndVerifyProduct(client, compositeProduct.getProductId(), OK);
    }

    @Test
    public void getProductById(@Autowired WebTestClient client) {

        getAndVerifyProduct(client, PRODUCT_ID_OK, OK)
                .jsonPath("$.productId").isEqualTo(PRODUCT_ID_OK)
                .jsonPath("$.recommendations.length()").isEqualTo(1)
                .jsonPath("$.reviews.length()").isEqualTo(1);
    }

    @Test
    public void getProductNotFound(@Autowired WebTestClient client) {

        getAndVerifyProduct(client, PRODUCT_ID_NOT_FOUND, NOT_FOUND)
                .jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_NOT_FOUND)
                .jsonPath("$.message").isEqualTo("NOT FOUND: " + PRODUCT_ID_NOT_FOUND);
    }

    @Test
    public void getProductInvalidInput(@Autowired WebTestClient client) {

        getAndVerifyProduct(client, PRODUCT_ID_INVALID, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/product-composite/" + PRODUCT_ID_INVALID)
                .jsonPath("$.message").isEqualTo("INVALID: " + PRODUCT_ID_INVALID);
    }

    private WebTestClient.BodyContentSpec getAndVerifyProduct(WebTestClient client, int productId, HttpStatus expectedStatus) {
        return client.get()
                .uri("/product-composite/" + productId)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }

    private void postAndVerifyProduct(WebTestClient client, ProductAggregate compositeProduct, HttpStatus expectedStatus) {
        client.post()
                .uri("/product-composite")
                .body(just(compositeProduct), ProductAggregate.class)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }

    private void deleteAndVerifyProduct(WebTestClient client, int productId, HttpStatus expectedStatus) {
        client.delete()
                .uri("/product-composite/" + productId)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus);
    }
}