package com.study.part08_ioc.s04_strategy;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * [8.4] '연결 만들기' 책임을 담는 인터페이스 (전략 패턴의 Strategy).
 *
 * 8.3에서 본 상속의 한계(단일 상속·컴파일 타임 결합·강결합)의 공통 원인은 '상속'이었다. 8.4는 상속 대신
 * '인터페이스 + 합성(composition)'으로 전환한다. UserDao는 이 인터페이스에만 의존하고(구체 구현은 모름),
 * 실제 구현체는 외부에서 주입받는다.
 *
 * 전략 패턴 용어 매핑: 이 인터페이스 = Strategy(갈아끼울 수 있는 알고리즘 = '연결 방법').
 * 인터페이스라 여러 개 구현 가능 -> 단일 상속 제약(8.3 한계①)에서 자유롭다.
 */
public interface ConnectionMaker {
    Connection makeConnection() throws ClassNotFoundException, SQLException;
}
