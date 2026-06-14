package com.study.part08_ioc.stage3_template_method;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * [STAGE 3] 'N 고객사'용 자식 — 변하는 부분(연결 방법)만 구현한다.
 *
 * 부모(UserDao)의 add/get 흐름은 그대로 물려받고, getConnection만 자기 방식으로 채운다. 다른 고객사는
 * DUserDao처럼 또 다른 자식을 만들면 된다. 부모(핵심 흐름)는 건드리지 않는다(템플릿 메소드의 이점).
 * (데모라 둘 다 H2를 쓰되 DB 이름만 달리해 '서로 다른 연결'임을 흉내 낸다.)
 */
public class NUserDao extends UserDao {
    @Override
    protected Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection("jdbc:h2:mem:stage3_n;DB_CLOSE_DELAY=-1", "sa", "");
    }
}
