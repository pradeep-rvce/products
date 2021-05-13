package com.handson.productservice.persistence;

import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Mono;

public interface ProductRepository extends ReactiveSortingRepository<ProductEntity, String> {
    Mono<ProductEntity> findByProductId(int productId);
}