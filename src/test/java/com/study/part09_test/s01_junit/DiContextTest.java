package com.study.part09_test.s01_junit;

import com.study.part08_ioc.s04_strategy.UserDao;
import com.study.part08_ioc.s06_appcontext_di.DaoFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [9.1] PART 8.6의 IoC/DI(ApplicationContext·싱글톤)를 '테스트로' 검증한다.
 *
 * 8.6에서는 main()에 println으로 'dao1 == dao2'를 찍어 사람이 확인했다. 이제 그 검증을 자동화 테스트로
 * 옮긴다 — 컨테이너가 DI로 빈을 완성해 주는지, 그리고 빈이 싱글톤인지를 단언으로 확인한다.
 */
class DiContextTest {

    private AnnotationConfigApplicationContext ctx;

    @BeforeEach
    void setUp() {
        // 설정(DaoFactory)을 읽어 컨테이너를 띄운다. 이 순간 빈 생성 + 의존관계 주입(DI)이 일어난다.
        ctx = new AnnotationConfigApplicationContext(DaoFactory.class);
    }

    @AfterEach
    void tearDown() {
        ctx.close();   // 각 테스트 후 컨테이너 정리(자원 해제)
    }

    @Test
    @DisplayName("컨테이너가 DI로 완성한 UserDao 빈을 꺼낼 수 있다")
    void getBeanReturnsWiredDao() {
        UserDao dao = ctx.getBean("userDao", UserDao.class);
        assertThat(dao).isNotNull();   // 컨테이너가 ConnectionMaker까지 주입해 완성한 빈
    }

    @Test
    @DisplayName("같은 빈을 여러 번 꺼내도 동일 인스턴스다(싱글톤 레지스트리)")
    void beanIsSingleton() {
        UserDao dao1 = ctx.getBean("userDao", UserDao.class);
        UserDao dao2 = ctx.getBean("userDao", UserDao.class);

        // == 비교: 같은 '인스턴스'인지. 싱글톤이면 true. (8.6에서 println으로 보던 것을 자동 검증으로)
        assertThat(dao1).isSameAs(dao2);
    }
}
