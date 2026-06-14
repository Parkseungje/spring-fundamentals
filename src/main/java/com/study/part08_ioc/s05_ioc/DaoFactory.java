package com.study.part08_ioc.s05_ioc;

import com.study.part08_ioc.s04_strategy.ConnectionMaker;
import com.study.part08_ioc.s04_strategy.NConnectionMaker;
import com.study.part08_ioc.s04_strategy.UserDao;

/**
 * [8.5] 조립(생성 + 주입) 책임을 가져가는 '팩토리' — 아직 Spring이 아닌 '순수 자바' 버전.
 *
 * 8.4까지의 문제: UserDao는 깨끗해졌지만, 'new NConnectionMaker()'를 만들어 'new UserDao(cm)'로 넣는
 * 조립 책임이 클라이언트(Main)에 있었다. 즉 Main이 "어떤 구현을 쓸지"를 여전히 결정했다.
 *
 * 8.5의 한 걸음: 그 조립 책임을 이 DaoFactory로 '분리'한다. 이제 Main은 무엇을 new 할지 모르고,
 * 그냥 factory.userDao()로 '완성된 객체'를 받아 쓴다. → "어떤 구현을 쓸지"의 결정권(제어권)이 Main에서
 * 팩토리(외부)로 넘어갔다 = IoC(제어의 역전)의 출발점.
 *
 * 이 클래스는 아직 Spring 어노테이션이 없는 '평범한 자바 클래스'다. 8.6에서 여기에 @Configuration/@Bean을
 * 붙여 Spring 컨테이너가 이 조립을 대신 수행하게 만든다(같은 개념을 프레임워크가 떠안는 것).
 */
public class DaoFactory {

    // '무엇을 만들어 어떻게 연결할지'(조립)를 여기에 모은다. Main은 이 결정에 관여하지 않는다.
    public UserDao userDao() {
        return new UserDao(connectionMaker());   // 생성 + 주입을 팩토리가 수행
    }

    public ConnectionMaker connectionMaker() {
        return new NConnectionMaker();           // D로 바꾸려면 '여기'만 고치면 됨(클라이언트는 무관)
    }
}
