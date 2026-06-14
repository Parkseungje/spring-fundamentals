package com.study.part08_ioc.stage4_strategy;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * [STAGE 4 / 8.4] '연결 만들기' 책임을 담는 인터페이스 (전략 패턴의 Strategy).
 *
 * stage3는 상속으로 '연결 방법'을 분리했지만, 단일 상속 제약·컴파일 타임 결합이라는 한계가 있었다.
 * stage4는 상속 대신 '인터페이스 + 합성(composition)'으로 전환한다. UserDao는 이 인터페이스에만
 * 의존하고(구체 구현은 모름), 실제 구현체는 외부에서 주입받는다.
 *
 * 전략 패턴 용어 매핑: 이 인터페이스 = Strategy(갈아끼울 수 있는 알고리즘 = 연결 방법).
 */
public interface ConnectionMaker {
    Connection makeConnection() throws ClassNotFoundException, SQLException;
}
