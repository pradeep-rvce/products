package com.handson.reviewservice;

import com.handson.api.cpre.review.Review;
import com.handson.reviewservice.persistence.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;

@SpringBootTest(webEnvironment=RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:review-db"})

public class ReviewServiceApplicationTests {

    @Autowired
    private ReviewRepository repository;


    @BeforeEach
    public void setupDb() {
        repository.deleteAll();
    }

    @Test
    public void getReviewsByProductId(@Autowired WebTestClient client) {

        int productId = 1;

        assertEquals(0, repository.findByProductId(productId).size());

        postAndVerifyReview(client, productId, 1, OK);
        postAndVerifyReview(client, productId, 2, OK);
        postAndVerifyReview(client, productId, 3, OK);

        assertEquals(3, repository.findByProductId(productId).size());

        getAndVerifyReviewsByProductId(client, productId, OK)
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[2].productId").isEqualTo(productId)
                .jsonPath("$[2].reviewId").isEqualTo(3);
    }

    @Test
    public void duplicateError(@Autowired WebTestClient client) {

        int productId = 1;
        int reviewId = 1;

        assertEquals(0, repository.count());

        postAndVerifyReview(client, productId, reviewId, OK)
                .jsonPath("$.productId").isEqualTo(productId)
                .jsonPath("$.reviewId").isEqualTo(reviewId);

        assertEquals(1, repository.count());

        postAndVerifyReview(client, productId, reviewId, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/review")
                .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: 1, Review Id:1");

        assertEquals(1, repository.count());
    }

    @Test
    public void deleteReviews(@Autowired WebTestClient client) {

        int productId = 1;
        int recommendationId = 1;

        postAndVerifyReview(client, productId, recommendationId, OK);
        assertEquals(1, repository.findByProductId(productId).size());

        deleteAndVerifyReviewsByProductId(client, productId, OK);
        assertEquals(0, repository.findByProductId(productId).size());

        deleteAndVerifyReviewsByProductId(client, productId, OK);
    }

    @Test
    public void getReviewsMissingParameter(@Autowired WebTestClient client) {

        getAndVerifyReviewsByProductId(client, "", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/review")
                .jsonPath("$.message").isEqualTo("Required int parameter 'productId' is not present");
    }

    @Test
    public void getReviewsInvalidParameter(@Autowired WebTestClient client) {

        getAndVerifyReviewsByProductId(client, "?productId=no-integer", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/review")
                .jsonPath("$.message").isEqualTo("Type mismatch.");
    }

    @Test
    public void getReviewsNotFound(@Autowired WebTestClient client) {

        getAndVerifyReviewsByProductId(client, "?productId=213", OK)
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    public void getReviewsInvalidParameterNegativeValue(@Autowired WebTestClient client) {

        int productIdInvalid = -1;

        getAndVerifyReviewsByProductId(client, "?productId=" + productIdInvalid, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/review")
                .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
    }

    private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(WebTestClient client, int productId, HttpStatus expectedStatus) {
        return getAndVerifyReviewsByProductId(client, "?productId=" + productId, expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(WebTestClient client, String productIdQuery, HttpStatus expectedStatus) {
        return client.get()
                .uri("/review" + productIdQuery)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }

    private WebTestClient.BodyContentSpec postAndVerifyReview(WebTestClient client, int productId, int reviewId, HttpStatus expectedStatus) {
        Review review = new Review(productId, reviewId, "Author " + reviewId, "Subject " + reviewId, "Content " + reviewId, "SA");
        return client.post()
                .uri("/review")
                .body(just(review), Review.class)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }

    private WebTestClient.BodyContentSpec deleteAndVerifyReviewsByProductId(WebTestClient client, int productId, HttpStatus expectedStatus) {
        return client.delete()
                .uri("/review?productId=" + productId)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectBody();
    }
}