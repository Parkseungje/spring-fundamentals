package com.study.part08_ioc.stage5_ioc_container;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [STAGE 5 / 8.6] 객체 생성·연결을 책임지는 설정 클래스 (IoC 컨테이너의 '설계도').
 *
 * stage4에서 Main이 직접 하던 'new + 주입' 조립을 이 클래스로 옮긴다. 그리고 이 조립을 'Spring 컨테이너'가
 * 대신 실행하게 맡긴다(IoC = 제어의 역전 — 객체 생성·연결의 '제어권'이 내 코드에서 컨테이너로 넘어감).
 *
 * 핵심 어노테이션:
 *   - @Configuration: "이 클래스는 빈 설정(설계도)이다"라고 Spring에 알림.
 *   - @Bean: "이 메서드가 반환하는 객체를 Spring이 관리하는 빈으로 등록하라". 메서드 이름이 빈 이름이 된다.
 *
 * 의존관계 주입(DI)이 여기서 일어난다: userDao() 안에서 connectionMaker()를 호출해 UserDao 생성자에 넣는다.
 * → "UserDao는 ConnectionMaker가 필요하다"는 의존관계를 컨테이너가 런타임에 연결해준다.
 *
 * ★ 싱글톤: @Bean 메서드를 코드상 여러 번 호출하는 것처럼 보여도, Spring은 각 빈을 '한 개만' 만들어
 *   재사용한다(싱글톤 레지스트리). 그래서 userDao를 여러 번 getBean 해도 같은 인스턴스다(Main에서 확인).
 */
@Configuration
public class DaoFactory {

    @Bean
    public UserDao userDao() {
        // DI: 컨테이너가 connectionMaker 빈을 만들어 UserDao에 주입한다(조립 책임이 여기로 모임).
        return new UserDao(connectionMaker());
    }

    @Bean
    public ConnectionMaker connectionMaker() {
        return new ConnectionMaker.HConnectionMaker();
    }
}
