package com.study.part08_ioc.s06_appcontext_di;

import com.study.part08_ioc.s04_strategy.ConnectionMaker;
import com.study.part08_ioc.s04_strategy.NConnectionMaker;
import com.study.part08_ioc.s04_strategy.UserDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [8.6] 8.5의 '평범한 자바 팩토리'에 Spring 어노테이션을 붙여 'IoC 컨테이너의 설계도'로 만든다.
 *
 * 8.5 DaoFactory와 코드 모양은 거의 같다. 차이는 단 두 어노테이션:
 *   - @Configuration: "이 클래스는 빈 설정(설계도)이다"라고 Spring에 알림.
 *   - @Bean: "이 메서드가 반환하는 객체를 Spring이 관리하는 '빈'으로 등록하라". 메서드 이름이 빈 이름이 된다.
 *
 * 이제 조립(생성+주입)을 '내가 직접' 호출하지 않는다. Spring 컨테이너(ApplicationContext)가 이 설계도를
 * 읽어 빈을 만들고, 의존관계를 주입한다(= 8.5의 IoC를 '프레임워크'가 떠안음).
 *
 * 의존관계 주입(DI): userDao()가 connectionMaker()를 호출해 UserDao 생성자에 넣는다 → "UserDao는
 * ConnectionMaker가 필요하다"는 의존관계를 컨테이너가 런타임에 연결해 준다.
 *
 * ★ 싱글톤: @Bean 메서드가 코드상 여러 번 호출되는 것처럼 보여도, Spring은 각 빈을 '딱 하나'만 만들어
 *   재사용한다(싱글톤 레지스트리). 그래서 userDao를 몇 번 getBean 해도 같은 인스턴스다(Main에서 확인).
 */
@Configuration
public class DaoFactory {

    @Bean
    public UserDao userDao() {
        // DI: 컨테이너가 connectionMaker 빈을 만들어 UserDao에 주입한다(조립 책임이 컨테이너로).
        return new UserDao(connectionMaker());
    }

    // ★ connectionMaker는 지금 userDao()에서만 쓰이는데도 @Bean을 붙인다. 왜?
    //   ① 싱글톤 보장(가장 중요): @Bean이면 connectionMaker()를 여러 번 호출해도 Spring이 객체를 '하나만'
    //      만들어 반환한다(@Configuration이 프록시로 메서드 호출을 가로채 캐시함). 나중에 DAO가 늘어 여러
    //      @Bean이 connectionMaker()를 호출해도 같은 인스턴스를 공유한다. @Bean이 없다면 호출마다 new가 실행돼
    //      매번 새 객체가 생긴다(커넥션 풀 등을 공유 못 함).
    //   ② 다른 빈들이 공유·재사용 가능, getBean으로 단독으로 꺼내거나 교체·테스트하기 쉬움.
    //   ③ 모든 부품을 컨테이너가 일관되게 관리(어떤 건 빈, 어떤 건 new로 섞이면 관리가 어려움).
    @Bean
    public ConnectionMaker connectionMaker() {
        return new NConnectionMaker();
    }
}
