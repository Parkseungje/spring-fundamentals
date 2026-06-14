package com.study.part08_ioc.stage3_template_method;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * [STAGE 3] 'D 고객사'용 자식 — NUserDao와 다른 연결 방법만 갈아끼운다.
 *
 * 새 고객사(D)가 추가돼도 부모 UserDao의 add/get은 한 줄도 안 바뀐다 — 자식 클래스 하나만 추가.
 * 다만 "어떤 자식을 쓸지"가 결국 'new NUserDao()' 또는 'new DUserDao()'처럼 코드에 박힌다(컴파일 타임 결합).
 * 이 한계를 stage4(인터페이스+합성)에서 푼다.
 */
public class DUserDao extends UserDao {
    @Override
    protected Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection("jdbc:h2:mem:stage3_d;DB_CLOSE_DELAY=-1", "sa", "");
    }
}
