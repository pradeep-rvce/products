package com.handson.reviewservice;

import com.handson.api.cpre.event.Event;
import com.handson.api.cpre.product.Product;
import com.handson.api.cpre.review.Review;
import com.handson.reviewservice.persistence.ReviewRepository;
import com.handson.util.exception.InvalidInputException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.http.HttpStatus;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.handson.api.cpre.event.Event.Type.CREATE;
import static com.handson.api.cpre.event.Event.Type.DELETE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(webEnvironment=RANDOM_PORT, properties = {"spring.datasource.url=jdbc:h2:mem:review-db", "spring.data.mongodb.port: 0", "eureka.client.enabled=false", "spring.cloud.config.enabled=false", "server.error.include-message=always"})
@ActiveProfiles("test")
public class ReviewServiceApplicationTests {

    private static WebTestClient client;

    @Autowired
    private ReviewRepository repository;

    @Autowired
    private Sink channels;

    private AbstractMessageChannel input = null;

    @BeforeAll
    public static void setUp(@Autowired WebTestClient client1) {
        client = client1;
    }

    @BeforeEach
    public void setupDb() {
        input = (AbstractMessageChannel) channels.input();
        repository.deleteAll();
    }

    @Test
    public void getReviewsByProductId() {

        int productId = 1;

        assertEquals(0, repository.findByProductId(productId).size());

        sendCreateReviewEvent(productId, 1);
        sendCreateReviewEvent(productId, 2);
        sendCreateReviewEvent(productId, 3);

        assertEquals(3, repository.findByProductId(productId).size());

        getAndVerifyReviewsByProductId(productId, OK)
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[2].productId").isEqualTo(productId)
                .jsonPath("$[2].reviewId").isEqualTo(3);
    }

    @Test
    public void duplicateError() {

        int productId = 1;
        int reviewId = 1;

        assertEquals(0, repository.count());

        sendCreateReviewEvent(productId, reviewId);

        assertEquals(1, repository.count());

        try {
            sendCreateReviewEvent(productId, reviewId);
            fail("Expected a MessagingException here!");
        } catch (MessagingException me) {
            if (me.getCause() instanceof InvalidInputException) {
                InvalidInputException iie = (InvalidInputException) me.getCause();
                assertEquals("Duplicate key, Product Id: 1, Review Id:1", iie.getMessage());
            } else {
                fail("Expected a InvalidInputException as the root cause!");
            }
        }

        assertEquals(1, repository.count());
    }

    @Test
    public void deleteReviews() {

        int productId = 1;
        int reviewId = 1;

        sendCreateReviewEvent(productId, reviewId);
        assertEquals(1, repository.findByProductId(productId).size());

        sendDeleteReviewEvent(productId);
        assertEquals(0, repository.findByProductId(productId).size());

        sendDeleteReviewEvent(productId);
    }

    @Test
    public void getReviewsMissingParameter() {

        getAndVerifyReviewsByProductId("", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/review")
                .jsonPath("$.message").isEqualTo("Required int parameter 'productId' is not present");
    }

    @Test
    public void getReviewsInvalidParameter() {

        getAndVerifyReviewsByProductId("?productId=no-integer", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/review")
                .jsonPath("$.message").isEqualTo("Type mismatch.");
    }

    @Test
    public void getReviewsNotFound() {

        getAndVerifyReviewsByProductId("?productId=213", OK)
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    public void getReviewsInvalidParameterNegativeValue() {

        int productIdInvalid = -1;

        getAndVerifyReviewsByProductId("?productId=" + productIdInvalid, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/review")
                .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
    }

    private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(int productId, HttpStatus expectedStatus) {
        return getAndVerifyReviewsByProductId("?productId=" + productId, expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyReviewsByProductId(String productIdQuery, HttpStatus expectedStatus) {
        return client.get()
                .uri("/review" + productIdQuery)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }

    private void sendCreateReviewEvent(int productId, int reviewId) {
        Review review = new Review(productId, reviewId, "Author " + reviewId, "Subject " + reviewId, "Content " + reviewId, "SA");
        Event<Integer, Product> event = new Event(CREATE, productId, review);
        input.send(new GenericMessage<>(event));
    }

    private void sendDeleteReviewEvent(int productId) {
        Event<Integer, Product> event = new Event(DELETE, productId, null);
        input.send(new GenericMessage<>(event));
    }
}