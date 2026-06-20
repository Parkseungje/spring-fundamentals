package com.study.part11_jpa.s03_jpa_intro;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

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

    @Autowired
    TestEntityManager em;            // flush/clear로 영속성 컨텍스트를 직접 제어(영속성 컨텍스트 데모용)

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

    @Test
    @DisplayName("영속성 컨텍스트(1차 캐시) — 같은 PK를 두 번 조회하면 '같은 인스턴스'(== true)")
    void firstLevelCacheIdentity() {
        Item saved = itemRepository.save(new Item("커피", 4500));
        Long id = saved.getId();

        // 같은 트랜잭션(=같은 영속성 컨텍스트) 안에서 같은 PK를 두 번 조회한다.
        Item a = itemRepository.findById(id).orElseThrow();
        Item b = itemRepository.findById(id).orElseThrow();

        // JPA는 1차 캐시에 보관한 '같은 인스턴스'를 돌려준다 -> 자바 '=='도 true.
        // 11.2의 식별 미스매치(raw JDBC는 new를 두 번 해 == false였던 것)를 영속성 컨텍스트가 해소한 모습.
        assertThat(a).isSameAs(b);          // == true (동일 인스턴스)
        // (두 번째 findById는 DB를 다시 안 치고 1차 캐시에서 반환 -> show-sql 로그에도 select가 한 번만 찍힌다.)
    }

    @Test
    @DisplayName("변경 감지(Dirty Checking) — save() 없이 필드만 바꿔도 update가 자동 실행된다")
    void dirtyChecking() {
        Item saved = itemRepository.save(new Item("커피", 4500));
        Long id = saved.getId();

        // 영속 상태 엔티티의 값만 바꾼다. itemRepository.save(...)나 update SQL을 '호출하지 않는다'.
        saved.changePrice(9900);

        // flush: 영속성 컨텍스트가 변경을 감지해 update SQL을 생성·실행하는 시점(보통 트랜잭션 커밋 때 자동).
        em.flush();
        em.clear();   // 1차 캐시 비우기 -> 다음 조회는 DB에서 새로 읽어 실제 반영 여부 확인

        Item reloaded = itemRepository.findById(id).orElseThrow();
        assertThat(reloaded.getPrice()).isEqualTo(9900);   // update를 안 썼는데 9900으로 반영됨!
    }

    @Test
    @DisplayName("JPQL @Query — 메서드 이름으로 표현하기 복잡한 조건은 JPQL로 직접 작성")
    void jpqlQuery() {
        itemRepository.save(new Item("커피", 4500));
        itemRepository.save(new Item("커피", 6000));
        itemRepository.save(new Item("케이크", 6000));

        // JPQL: 엔티티 Item과 필드(name/price)를 대상으로 쓴 객체 지향 쿼리 -> JPA가 SQL로 번역.
        List<Item> result = itemRepository.searchByNameAndMinPrice("커피", 5000);
        assertThat(result).extracting(Item::getPrice).containsExactly(6000);  // 커피 && price>=5000
    }
}
