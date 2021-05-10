package com.handson.productservice;

import com.handson.api.cpre.product.Product;
import com.handson.productservice.persistence.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static reactor.core.publisher.Mono.just;


@SpringBootTest(webEnvironment=RANDOM_PORT)
public class ProductServiceApplicationTests {

    @Autowired
    private ProductRepository repository;

    @BeforeEach
    public void setupDb() {
        repository.deleteAll();
    }

    @Test
    public void getProductById(@Autowired WebTestClient client) {

        int productId = 1;

        postAndVerifyProduct(client, productId, OK);

        assertTrue(repository.findByProductId(productId).isPresent());

        getAndVerifyProduct(client, productId, OK)
                .jsonPath("$.productId").isEqualTo(productId);
    }

    @Test
    public void duplicateError(@Autowired WebTestClient client) {

        int productId = 1;

        postAndVerifyProduct(client, productId, OK);

        assertTrue(repository.findByProductId(productId).isPresent());

        postAndVerifyProduct(client, productId, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/product")
                .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: " + productId);
    }

    @Test
    public void deleteProduct(@Autowired WebTestClient client) {

        int productId = 1;

        postAndVerifyProduct(client, productId, OK);
        assertTrue(repository.findByProductId(productId).isPresent());

        deleteAndVerifyProduct(client, productId, OK);
        assertFalse(repository.findByProductId(productId).isPresent());

        deleteAndVerifyProduct(client, productId, OK);
    }

    @Test
    public void getProductInvalidParameterString(@Autowired WebTestClient client) {

        getAndVerifyProduct(client, "/no-integer", BAD_REQUEST)
                .jsonPath("$.path").isEqualTo("/product/no-integer")
                .jsonPath("$.message").isEqualTo("Type mismatch.");
    }

    @Test
    public void getProductNotFound(@Autowired WebTestClient client) {

        int productIdNotFound = 13;
        getAndVerifyProduct(client, productIdNotFound, NOT_FOUND)
                .jsonPath("$.path").isEqualTo("/product/" + productIdNotFound)
                .jsonPath("$.message").isEqualTo("No product found for productId: " + productIdNotFound);
    }

    @Test
    public void getProductInvalidParameterNegativeValue(@Autowired WebTestClient client) {

        int productIdInvalid = -1;

        getAndVerifyProduct(client, productIdInvalid, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/product/" + productIdInvalid)
                .jsonPath("$.message").isEqualTo("Invalid productId: " + productIdInvalid);
    }

    private WebTestClient.BodyContentSpec getAndVerifyProduct(WebTestClient client, int productId, HttpStatus expectedStatus) {
        return getAndVerifyProduct(client, "/" + productId, expectedStatus);
    }

    private WebTestClient.BodyContentSpec getAndVerifyProduct(WebTestClient client, String productIdPath, HttpStatus expectedStatus) {
        return client.get()
                .uri("/product" + productIdPath)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }

    private WebTestClient.BodyContentSpec postAndVerifyProduct(WebTestClient client, int productId, HttpStatus expectedStatus) {
        Product product = new Product(productId, "Name " + productId, productId, "SA");
        return client.post()
                .uri("/product")
                .body(just(product), Product.class)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON)
                .expectBody();
    }

    private WebTestClient.BodyContentSpec deleteAndVerifyProduct(WebTestClient client, int productId, HttpStatus expectedStatus) {
        return client.delete()
                .uri("/product/" + productId)
                .accept(APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectBody();
    }

}