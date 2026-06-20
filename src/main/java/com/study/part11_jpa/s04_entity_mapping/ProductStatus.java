package com.study.part11_jpa.s04_entity_mapping;

/**
 * [11.4] 상품 상태 enum — @Enumerated 매핑 데모용.
 *
 * 이 enum을 DB에 어떻게 저장하느냐가 @Enumerated의 핵심:
 *   - EnumType.STRING  : 이름 그대로 저장("ON_SALE"). 사람이 읽기 쉽고, 순서가 바뀌어도 안전.
 *   - EnumType.ORDINAL : 선언 순서 숫자로 저장(ON_SALE=0, SOLD_OUT=1, ...). ★ 함정: 나중에 상수 순서를
 *     바꾸거나 중간에 새 상수를 끼워 넣으면, 이미 저장된 숫자의 '의미'가 통째로 어긋난다(데이터 깨짐).
 * 그래서 실무는 거의 항상 STRING을 쓴다(Product에서 STRING으로 매핑).
 */
public enum ProductStatus {
    ON_SALE,       // 판매 중
    SOLD_OUT,      // 품절
    DISCONTINUED   // 단종
}
