package com.study.part08_ioc.s04_strategy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * [8.4] N 고객사용 연결 구현 (전략 패턴의 ConcreteStrategy).
 *
 * 8.2의 NUserDao가 'UserDao를 상속'했던 것과 달리, 이제는 ConnectionMaker '인터페이스를 구현'할 뿐
 * UserDao와는 상속 관계가 없다. 그래서 UserDao와 독립적으로 자유롭게 만들고 갈아끼울 수 있다.
 */
public class NConnectionMaker implements ConnectionMaker {
    @Override
    public Connection makeConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection("jdbc:h2:mem:part08_s04;DB_CLOSE_DELAY=-1", "sa", "");
    }
}
