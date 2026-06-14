package com.study.part09_test.s01_junit;

import com.study.part08_ioc.domain.User;
import com.study.part08_ioc.s04_strategy.NConnectionMaker;
import com.study.part08_ioc.s04_strategy.UserDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * [9.1] JUnit 테스트 — PART 8에서 만든 UserDao(IoC/DI 코드)를 '자동화 테스트'로 검증한다.
 *
 * 왜 테스트인가: main()으로 println을 찍어 '사람이 눈으로' 확인하는 방식은 매번 반복·비효율이고 실수가
 * 난다. 단위 테스트는 '코드가 코드를 검증'한다 — 자동화·격리·빠름·반복 가능. 한 번 짜두면 회귀(이전에
 * 되던 게 깨짐)도 즉시 잡는다.
 *
 * 이 테스트가 보여주는 것:
 *   - assertThat 매처: 기대값과 실제값을 자연어처럼 단언(AssertJ / Hamcrest 둘 다 사용해 비교).
 *   - 픽스처(@BeforeEach): 각 테스트 전에 테이블 생성 + deleteAll로 '시작 상태'를 보장(테스트 간 독립성).
 *   - @Test마다 새 인스턴스: JUnit은 테스트 메서드마다 이 클래스의 인스턴스를 새로 만든다(아래 instanceId로 확인).
 */
class UserDaoTest {

    private UserDao dao;
    private User user1;
    private User user2;

    // ★ 픽스처: 각 @Test 실행 '직전'마다 호출된다. 매번 깨끗한 시작 상태를 만들어 테스트 간 독립을 보장한다.
    @BeforeEach
    void setUp() throws Exception {
        dao = new UserDao(new NConnectionMaker());   // PART 8.4의 DAO를 그대로 테스트 대상으로
        dao.createTable();
        dao.deleteAll();                              // 이전 테스트가 남긴 데이터 제거(격리)
        user1 = new User("t1", "테스터1");
        user2 = new User("t2", "테스터2");
    }

    @Test
    @DisplayName("저장한 사용자를 id로 조회하면 같은 데이터가 나온다")
    void addAndGet() throws Exception {
        dao.add(user1);

        User found = dao.get("t1");

        // AssertJ 스타일: assertThat(실제).isEqualTo(기대) — 메서드 체이닝, 가독성 좋음(스프링 기본).
        assertThat(found.getId()).isEqualTo("t1");
        assertThat(found.getName()).isEqualTo("테스터1");

        // Hamcrest 스타일: assertThat(실제, is(기대)) — 자연어처럼 읽힘(커리큘럼에서 소개한 매처).
        assertThat(found.getName(), is("테스터1"));
    }

    @Test
    @DisplayName("deleteAll 후 count는 0, add할수록 count가 증가한다")
    void countReflectsState() throws Exception {
        assertThat(dao.getCount()).isEqualTo(0);   // setUp에서 deleteAll 했으므로 시작은 0

        dao.add(user1);
        assertThat(dao.getCount()).isEqualTo(1);

        dao.add(user2);
        assertThat(dao.getCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("이 테스트는 위 테스트의 영향을 받지 않는다(독립성) — 시작 count는 항상 0")
    void independentFromOtherTests() throws Exception {
        // 다른 테스트에서 user를 add 했더라도, @BeforeEach의 deleteAll 덕분에 여기선 항상 0에서 시작한다.
        assertThat(dao.getCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("없는 id 조회는 예외가 난다(rs.next()==false 상태에서 읽기 시도)")
    void getNotFoundThrows() {
        // 빈 결과에서 읽으려 하면 예외 — 예외 검증도 테스트로 자동화할 수 있다.
        assertThatThrownBy(() -> dao.get("nope"))
                .isInstanceOf(Exception.class);
    }
}
