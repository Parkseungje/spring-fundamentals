package com.study.part11_jpa.s03_jpa_intro;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * [11.3] Spring Data JPA 리포지토리 — "인터페이스만 만들면 구현은 Spring이 생성".
 *
 * JpaRepository<Item, Long>을 상속하는 '인터페이스'를 선언하기만 하면, Spring Data JPA가 런타임에 구현체를
 * 자동으로 만들어 빈으로 등록한다. 우리는 save/findById/findAll/delete 등을 구현 없이 바로 쓴다.
 *
 * ★ 쿼리 메서드(query method): 'findBy + 필드명' 규칙으로 메서드 이름만 지으면, Spring이 그 이름을 해석해
 *   SQL(JPQL)을 자동 생성한다. 예: findByName("커피") -> "name = '커피'" 조건의 select를 알아서 만든다.
 *   (SQL을 한 줄도 안 썼는데 조회가 된다 = JdbcTemplate(직접 SQL) 대비 JPA의 한 단계 높은 추상화.)
 */
public interface ItemRepository extends JpaRepository<Item, Long> {

    // 메서드 이름만으로 "where name = ?" 쿼리가 자동 생성된다(구현 코드 없음).
    List<Item> findByName(String name);

    // "where price >= ? order by price" 도 이름 규칙으로 자동 생성.
    List<Item> findByPriceGreaterThanEqualOrderByPriceAsc(int price);
}
