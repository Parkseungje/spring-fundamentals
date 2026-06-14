package com.study.part11_jpa.s04_entity_mapping;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * [11.4] Product용 Spring Data JPA 리포지토리(매핑 검증용).
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
