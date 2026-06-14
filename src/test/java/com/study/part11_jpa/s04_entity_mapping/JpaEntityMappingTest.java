package com.study.part11_jpa.s04_entity_mapping;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [11.4] 엔티티 매핑이 실제로 어떻게 적용되는지 검증한다.
 *   - @GeneratedValue(IDENTITY): save 후 PK 자동 채번.
 *   - 네이밍 전략: productName 필드 -> product_name 컬럼(네이티브 SQL로 확인).
 *   - @Column(nullable=false): null이면 저장 실패.
 *   - @Column(unique=true): 중복 code면 저장 실패.
 */
@DataJpaTest
class JpaEntityMappingTest {

    @Autowired ProductRepository productRepository;
    @Autowired EntityManager em;   // 네이티브 SQL로 '실제 컬럼명'을 확인하기 위해

    @Test
    @DisplayName("@GeneratedValue(IDENTITY) — save 후 PK가 자동 채번된다")
    void identityPk() {
        Product saved = productRepository.save(new Product("노트북", "P-001", 10));
        assertThat(saved.getId()).isNotNull();   // INSERT 후 DB가 채번한 PK
    }

    @Test
    @DisplayName("네이밍 전략 — productName 필드가 product_name 컬럼으로 매핑된다")
    void namingStrategy() {
        productRepository.save(new Product("노트북", "P-002", 10));
        productRepository.flush();

        // 컬럼명이 'product_name'이라야 이 네이티브 SQL이 성공한다(만약 productName이면 컬럼 없음 에러).
        Object count = em.createNativeQuery(
                "select count(*) from product where product_name = '노트북'").getSingleResult();
        assertThat(((Number) count).intValue()).isEqualTo(1);   // 자동 snake_case 변환 확인
    }

    @Test
    @DisplayName("@Column(nullable=false) — productName이 null이면 저장 실패")
    void notNullConstraint() {
        assertThatThrownBy(() ->
                productRepository.saveAndFlush(new Product(null, "P-003", 5)))
                .isInstanceOf(Exception.class);   // NOT NULL 제약 위반
    }

    @Test
    @DisplayName("@Column(unique=true) — 같은 code 두 번이면 저장 실패")
    void uniqueConstraint() {
        productRepository.saveAndFlush(new Product("상품A", "DUP", 1));
        assertThatThrownBy(() ->
                productRepository.saveAndFlush(new Product("상품B", "DUP", 1)))
                .isInstanceOf(Exception.class);   // UNIQUE 제약 위반
    }
}
