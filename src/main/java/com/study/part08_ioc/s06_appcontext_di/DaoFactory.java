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

    @Bean
    public ConnectionMaker connectionMaker() {
        return new NConnectionMaker();
    }
}
