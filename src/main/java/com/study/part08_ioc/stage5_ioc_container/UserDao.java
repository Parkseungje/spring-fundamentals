package com.study.part08_ioc.stage5_ioc_container;

import com.study.part08_ioc.domain.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * [STAGE 5] UserDao 자체는 stage4와 똑같다 — 여전히 ConnectionMaker에만 의존하고 생성자로 주입받는다.
 *
 * 바뀌는 건 UserDao가 아니라 '누가 이 객체를 만들고 누가 ConnectionMaker를 주입하느냐'다.
 * stage4: Main이 직접 new + 주입. stage5: Spring 컨테이너가 대신(IoC). 즉 UserDao 코드는 그대로인데
 * '조립 책임'만 컨테이너로 옮겨가는 것이 IoC의 핵심이다.
 */
public class UserDao {

    private final ConnectionMaker connectionMaker;

    public UserDao(ConnectionMaker connectionMaker) {
        this.connectionMaker = connectionMaker;
    }

    public void add(User user) throws ClassNotFoundException, SQLException {
        Connection c = connectionMaker.makeConnection();
        PreparedStatement ps = c.prepareStatement("insert into users(id, name) values (?, ?)");
        ps.setString(1, user.getId());
        ps.setString(2, user.getName());
        ps.executeUpdate();
        ps.close();
        c.close();
    }

    public User get(String id) throws ClassNotFoundException, SQLException {
        Connection c = connectionMaker.makeConnection();
        PreparedStatement ps = c.prepareStatement("select id, name from users where id = ?");
        ps.setString(1, id);
        ResultSet rs = ps.executeQuery();
        rs.next();
        User user = new User(rs.getString("id"), rs.getString("name"));
        rs.close();
        ps.close();
        c.close();
        return user;
    }

    public void createTable() throws ClassNotFoundException, SQLException {
        try (Connection c = connectionMaker.makeConnection()) {
            c.prepareStatement("create table if not exists users(id varchar(50) primary key, name varchar(50))")
                    .executeUpdate();
        }
    }
}
