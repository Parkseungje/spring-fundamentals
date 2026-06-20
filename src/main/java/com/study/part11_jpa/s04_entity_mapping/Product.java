package com.study.part11_jpa.s04_entity_mapping;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
// @Table: '테이블 단위' 옵션을 건다(@Column이 컬럼 단위인 것과 대비).
//  - uniqueConstraints: 여러 컬럼을 묶은 '복합 유니크'. @Column(unique=true)는 단일 컬럼만 가능하므로,
//    "이름 + 상태 조합은 유일"처럼 두 컬럼 조합 제약은 여기서 건다.
//  - indexes: 조회 성능용 인덱스 선언(PART 15~16에서 인덱스를 깊이 다룸).
@Entity
@Table(
        name = "product",
        uniqueConstraints = @UniqueConstraint(name = "uk_name_status", columnNames = {"product_name", "status"}),
        indexes = @Index(name = "idx_code", columnList = "code")
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // H2의 auto_increment로 PK 자동 채번
    private Long id;

    @Column(nullable = false, length = 50)               // NOT NULL + 최대 길이 50
    private String productName;                           // -> 컬럼 product_name (네이밍 전략 자동 변환)

    @Column(unique = true)                               // 단일 컬럼 UNIQUE 제약
    private String code;

    private int stockQuantity;                           // -> 컬럼 stock_quantity

    // enum 매핑: 반드시 STRING으로. (기본값 ORDINAL은 순서 숫자라 상수 순서가 바뀌면 기존 데이터가 깨진다.)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;                        // -> 컬럼 status, "ON_SALE" 같은 문자열로 저장

    // 값 타입: Address의 city/zipcode를 product 테이블의 컬럼으로 펼쳐 매핑(11.2 세분성 미스매치 해결).
    @Embedded
    private Address address;

    protected Product() {}   // 기본 생성자 필수(JPA 리플렉션용). 외부에서 막 만들지 못하게 protected.

    // 기존 생성자 — 상태 기본값(ON_SALE), 주소 없음. (기존 테스트 호환)
    public Product(String productName, String code, int stockQuantity) {
        this(productName, code, stockQuantity, ProductStatus.ON_SALE, null);
    }

    public Product(String productName, String code, int stockQuantity, ProductStatus status, Address address) {
        this.productName = productName;
        this.code = code;
        this.stockQuantity = stockQuantity;
        this.status = status;
        this.address = address;
    }

    public Long getId() { return id; }
    public String getProductName() { return productName; }
    public String getCode() { return code; }
    public int getStockQuantity() { return stockQuantity; }
    public ProductStatus getStatus() { return status; }
    public Address getAddress() { return address; }

    @Override
    public String toString() {
        return "Product{id=" + id + ", productName=" + productName + ", code=" + code
                + ", stockQuantity=" + stockQuantity + ", status=" + status + ", address=" + address + "}";
    }
}
