package com.study.part08_ioc.s04_strategy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * [8.4] D 고객사용 연결 구현 (또 다른 ConcreteStrategy).
 *
 * 새 연결 방식이 필요하면 이렇게 ConnectionMaker 구현체만 추가하면 된다. UserDao는 전혀 안 바뀐다
 * (확장에는 열림, 수정에는 닫힘 = OCP). 데모라 H2를 쓰되 DB 이름만 달리해 '다른 연결'을 흉내 낸다.
 */
public class DConnectionMaker implements ConnectionMaker {
    @Override
    public Connection makeConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection("jdbc:h2:mem:part08_s04;DB_CLOSE_DELAY=-1", "sa", "");
    }
}
