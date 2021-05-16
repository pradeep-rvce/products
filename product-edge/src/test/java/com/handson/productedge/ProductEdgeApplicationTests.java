package com.handson.productedge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.context.ActiveProfiles;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment=RANDOM_PORT, properties = {"spring.security.oauth2.resourceserver.jwt.jwk-set-uri: .","eureka.client.enabled=false","spring.cloud.config.enabled=false"})
class ProductEdgeApplicationTests {

    @Test
    void contextLoads() {
    }

}
