package com.study.part11_jpa.s03_jpa_intro;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * [11.3] JPA 입문용 엔티티 — "이 객체를 DB 테이블과 연결하라"는 표시.
 *
 * @Entity 하나로 JPA가 이 클래스를 'DB 테이블에 매핑되는 객체'로 관리한다. 그러면 우리가 SQL을 직접
 * 안 써도, JPA(Hibernate)가 insert/select 같은 SQL을 '대신 생성'해 준다(11.3의 핵심).
 *   - @Id: 기본 키(PK) 필드.
 *   - @GeneratedValue(IDENTITY): PK를 DB의 auto_increment로 자동 생성.
 * (엔티티 매핑의 세부 규칙 — 기본 생성자 필수, @Column 옵션, 생성 전략 종류 등 — 은 11.4에서 깊이 다룬다.
 *  테이블/컬럼 이름은 Spring Boot가 camelCase <-> snake_case로 자동 변환한다: Item -> item, name -> name.)
 */
@Entity
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private int price;

    protected Item() {}   // JPA가 리플렉션으로 객체를 만들 때 쓰는 기본 생성자(11.4에서 설명)

    public Item(String name, int price) {
        this.name = name;
        this.price = price;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getPrice() { return price; }

    @Override
    public String toString() {
        return "Item{id=" + id + ", name=" + name + ", price=" + price + "}";
    }
}
