package com.study.part08_ioc.stage5_ioc_container;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * [STAGE 5] 연결 전략 인터페이스 + 구현 (stage4와 동일 개념, IoC 데모용으로 이 패키지에 둠).
 *
 * stage5의 초점은 "이 객체들을 누가 만들고 누가 연결(주입)하느냐"이다. 그 답이 stage4에선 Main이었고,
 * stage5에선 'Spring 컨테이너(ApplicationContext)'다.
 */
public interface ConnectionMaker {
    Connection makeConnection() throws ClassNotFoundException, SQLException;

    // 데모 편의를 위해 기본 구현체를 한 파일에 함께 둔다(학습 흐름 집중).
    class HConnectionMaker implements ConnectionMaker {
        @Override
        public Connection makeConnection() throws ClassNotFoundException, SQLException {
            Class.forName("org.h2.Driver");
            return DriverManager.getConnection("jdbc:h2:mem:stage5;DB_CLOSE_DELAY=-1", "sa", "");
        }
    }
}
