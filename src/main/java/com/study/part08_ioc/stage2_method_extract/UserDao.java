package com.study.part08_ioc.stage2_method_extract;

import com.study.part08_ioc.domain.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * [STAGE 2 / 8.2-1단계] 관심사의 분리 — 1단계: 메서드 추출(extract method).
 *
 * stage1의 핵심 고통은 add/get에 '드라이버 로딩+접속' 코드가 중복된 것이었다. 이 중복(= 같은 관심사)을
 * private 메서드 getConnection() 한 곳으로 모은다. "관심이 같은 것끼리 모으고 다른 것은 떼어낸다"(= SRP).
 *
 * 무엇이 좋아졌나:
 *   - 중복 제거: 접속 코드가 한 곳(getConnection)에만 존재.
 *   - 변경 한 곳: DB 접속 정보가 바뀌어도 getConnection '한 메서드'만 고치면 add/get 모두 반영된다.
 *     (stage1은 모든 메서드를 고쳐야 했다 — 이 차이가 핵심.)
 *
 * 아직 남은 한계: getConnection이 여전히 'UserDao 안'에 있다. "DB마다 다른 접속 방식을 쓰되 UserDao
 * 코드는 안 건드리고 싶다"는 요구(예: 고객사마다 다른 DB)는 아직 못 푼다. -> stage3(상속/템플릿 메소드).
 */
public class UserDao {

    public void add(User user) throws ClassNotFoundException, SQLException {
        Connection c = getConnection();   // 중복이 사라지고 의도가 드러난다
        PreparedStatement ps = c.prepareStatement("insert into users(id, name) values (?, ?)");
        ps.setString(1, user.getId());
        ps.setString(2, user.getName());
        ps.executeUpdate();
        ps.close();
        c.close();
    }

    public User get(String id) throws ClassNotFoundException, SQLException {
        Connection c = getConnection();   // add와 '같은' 접속 코드를 재사용
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

    // ★ 분리된 '연결 만들기' 관심사 — DB 정보가 바뀌면 오직 이 한 곳만 고치면 된다.
    private Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection("jdbc:h2:mem:stage2;DB_CLOSE_DELAY=-1", "sa", "");
    }

    public void createTable() throws ClassNotFoundException, SQLException {
        try (Connection c = getConnection()) {
            c.prepareStatement("create table if not exists users(id varchar(50) primary key, name varchar(50))")
                    .executeUpdate();
        }
    }
}
