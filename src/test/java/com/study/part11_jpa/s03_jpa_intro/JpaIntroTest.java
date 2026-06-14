package com.study.part11_jpa.s03_jpa_intro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [11.3] JPA 입문 — SQL을 한 줄도 안 쓰고 저장·조회한다(JPA가 SQL을 대신 생성).
 *
 * @DataJpaTest: JPA 관련 빈(EntityManager, 리포지토리)만 띄우는 가벼운 테스트 슬라이스. 내장 H2를 쓰고,
 * 각 테스트는 트랜잭션 후 자동 롤백되어 격리된다(9.1의 테스트 독립성).
 *
 * 콘솔(show-sql=true)을 보면 우리가 안 쓴 insert/select SQL을 Hibernate가 자동 생성해 실행하는 걸 볼 수 있다.
 * 그것이 11.3의 핵심: "SQL Mapper(JdbcTemplate)는 SQL을 직접 썼지만, JPA는 SQL을 대신 써 준다."
 */
@DataJpaTest
class JpaIntroTest {

    @Autowired
    ItemRepository itemRepository;   // 인터페이스만 선언했는데 Spring이 구현체를 주입해 준다

    @Test
    @DisplayName("save/findById — SQL 없이 저장하고 PK로 조회한다(JPA가 insert/select 생성)")
    void saveAndFind() {
        Item saved = itemRepository.save(new Item("커피", 4500));   // insert SQL 자동 생성
        assertThat(saved.getId()).isNotNull();                      // IDENTITY로 PK 자동 채번

        Item found = itemRepository.findById(saved.getId()).orElseThrow();  // select 자동 생성
        assertThat(found.getName()).isEqualTo("커피");
        assertThat(found.getPrice()).isEqualTo(4500);
    }

    @Test
    @DisplayName("쿼리 메서드 — 메서드 이름(findByName)만으로 where 조건 SQL이 자동 생성된다")
    void queryMethod() {
        itemRepository.save(new Item("커피", 4500));
        itemRepository.save(new Item("케이크", 6000));
        itemRepository.save(new Item("커피", 5000));   // 같은 이름 2개

        List<Item> coffees = itemRepository.findByName("커피");     // where name = '커피'
        assertThat(coffees).hasSize(2);

        List<Item> over5000 = itemRepository.findByPriceGreaterThanEqualOrderByPriceAsc(5000);
        assertThat(over5000).extracting(Item::getPrice).containsExactly(5000, 6000);  // 가격 오름차순
    }
}
