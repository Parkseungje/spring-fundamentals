package com.study.part11_jpa.s04_entity_mapping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * [11.4] JPA 엔티티 매핑 — 객체와 테이블을 '어떻게' 연결할지 어노테이션으로 선언한다.
 *
 * @Entity 조건(중요):
 *   - 기본 생성자 필수: JPA가 리플렉션으로 객체를 만들 때 매개변수 없는 생성자를 호출한다(접근제어자는 protected까지 허용).
 *   - final 금지: JPA가 지연 로딩 등을 위해 엔티티를 '프록시'(상속 기반)로 감싸는데, final이면 상속을 못 해 막힌다.
 *
 * @Id: 기본 키(PK). 반드시 1개(또는 복합 키).
 * @GeneratedValue(전략): PK 자동 생성 방식.
 *   - IDENTITY : DB의 auto_increment 사용(MySQL/H2/PostgreSQL). INSERT 후에야 ID를 앎 -> 배치 INSERT 불가.
 *   - SEQUENCE : DB 시퀀스 객체 사용(Oracle/PostgreSQL). 미리 받아둬 빠르고 배치에 유리.
 *   - TABLE    : 키 생성용 테이블 사용(모든 DB 가능하나 느림).
 *   - AUTO     : DB에 맞게 자동 선택(기본값).
 *
 * @Column: 컬럼 세부 옵션(length/nullable/unique/columnDefinition 등).
 *
 * ★ 네이밍 전략: Spring Boot는 camelCase 필드 <-> snake_case 컬럼을 '자동' 변환한다(SpringPhysicalNamingStrategy).
 *   그래서 productName 필드는 별도 설정 없이 product_name 컬럼에 매핑된다(@Column(name=...) 생략 가능).
 */
@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // H2의 auto_increment로 PK 자동 채번
    private Long id;

    @Column(nullable = false, length = 50)               // NOT NULL + 최대 길이 50
    private String productName;                           // -> 컬럼 product_name (네이밍 전략 자동 변환)

    @Column(unique = true)                               // UNIQUE 제약
    private String code;

    private int stockQuantity;                           // -> 컬럼 stock_quantity

    protected Product() {}   // 기본 생성자 필수(JPA 리플렉션용). 외부에서 막 만들지 못하게 protected.

    public Product(String productName, String code, int stockQuantity) {
        this.productName = productName;
        this.code = code;
        this.stockQuantity = stockQuantity;
    }

    public Long getId() { return id; }
    public String getProductName() { return productName; }
    public String getCode() { return code; }
    public int getStockQuantity() { return stockQuantity; }

    @Override
    public String toString() {
        return "Product{id=" + id + ", productName=" + productName + ", code=" + code + ", stockQuantity=" + stockQuantity + "}";
    }
}
