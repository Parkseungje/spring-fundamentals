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

    @Test
    @DisplayName("@Enumerated(STRING) — enum이 숫자가 아니라 이름 문자열('ON_SALE')로 저장된다")
    void enumStringMapping() {
        productRepository.saveAndFlush(new Product("노트북", "E-1", 5, ProductStatus.SOLD_OUT, null));

        // status 컬럼 값을 네이티브로 직접 읽는다. STRING 매핑이면 'SOLD_OUT'(문자열), ORDINAL이면 1(숫자)이다.
        Object status = em.createNativeQuery(
                "select status from product where code = 'E-1'").getSingleResult();
        assertThat(status).isEqualTo("SOLD_OUT");   // 숫자 1이 아니라 이름으로 저장됨 = STRING 매핑
    }

    @Test
    @DisplayName("@Embedded — 값 타입 Address가 product 테이블의 city/zipcode 컬럼으로 펼쳐 저장된다")
    void embeddedValueType() {
        productRepository.saveAndFlush(
                new Product("모니터", "E-2", 3, ProductStatus.ON_SALE, new Address("서울", "06236")));

        // Address는 별도 테이블이 아니라 product 테이블의 city/zipcode 컬럼에 펼쳐진다(네이티브로 확인).
        Object[] row = (Object[]) em.createNativeQuery(
                "select city, zipcode from product where code = 'E-2'").getSingleResult();
        assertThat(row[0]).isEqualTo("서울");
        assertThat(row[1]).isEqualTo("06236");

        // 다시 엔티티로 읽으면 값 객체(Address)로 묶여 돌아온다.
        Product found = productRepository.findAll().stream()
                .filter(p -> "E-2".equals(p.getCode())).findFirst().orElseThrow();
        assertThat(found.getAddress().getCity()).isEqualTo("서울");
    }

    @Test
    @DisplayName("@Table 복합 유니크 — (product_name, status) 조합이 중복이면 저장 실패")
    void compositeUniqueConstraint() {
        // 단일 컬럼 unique가 아니라 '이름+상태 조합'이 유일해야 한다(@Column(unique=true)로는 못 거는 제약).
        productRepository.saveAndFlush(new Product("노트북", "C-1", 1, ProductStatus.ON_SALE, null));

        // 같은 이름이라도 '상태가 다르면' OK (조합이 다르니까)
        productRepository.saveAndFlush(new Product("노트북", "C-2", 1, ProductStatus.SOLD_OUT, null));

        // 같은 (이름=노트북, 상태=ON_SALE) 조합이면 복합 유니크 위반 -> 실패
        assertThatThrownBy(() ->
                productRepository.saveAndFlush(new Product("노트북", "C-3", 1, ProductStatus.ON_SALE, null)))
                .isInstanceOf(Exception.class);
    }
}
