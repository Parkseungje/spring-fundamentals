package com.study.part11_jpa.s04_entity_mapping;

import jakarta.persistence.Embeddable;

/**
 * [11.4] 값 타입(@Embeddable) — 11.2의 '세분성(granularity) 미스매치'를 푸는 매핑.
 *
 * 객체는 city/zipcode를 'Address'라는 한 덩어리(값 객체)로 잘게 표현하고 싶지만, DB는 보통 컬럼 두 개로
 * 펼친다(11.2). @Embeddable로 값 타입을 선언하고 엔티티에서 @Embedded로 품으면, JPA가 이 객체의 필드들을
 * '품은 엔티티의 테이블 컬럼'으로 펼쳐 매핑한다(별도 테이블이 아니라 product 테이블 안의 city/zipcode 컬럼).
 *
 * 값 타입은 식별자(@Id)가 없고, 그 자체로 독립 존재하지 않으며 엔티티에 소속된다(생명주기를 엔티티가 따라감).
 * 값 타입도 JPA가 리플렉션으로 생성하므로 기본 생성자가 필요하다.
 */
@Embeddable
public class Address {

    private String city;
    private String zipcode;

    protected Address() {}   // JPA 리플렉션용 기본 생성자

    public Address(String city, String zipcode) {
        this.city = city;
        this.zipcode = zipcode;
    }

    public String getCity() { return city; }
    public String getZipcode() { return zipcode; }

    @Override
    public String toString() {
        return "Address{city=" + city + ", zipcode=" + zipcode + "}";
    }
}
