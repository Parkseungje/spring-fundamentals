package com.study.part08_ioc.stage1_traditional;

import com.study.part08_ioc.domain.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * [STAGE 1 / 8.1] 전통 DAO — 모든 책임을 한 메서드가 떠안은 '고통' 버전.
 *
 * 이 단계가 보여주려는 것: "동작은 하지만 유지보수가 지옥인" 코드의 실체.
 * 한 메서드(add/get) 안에 아래 책임이 전부 뒤엉켜 있다.
 *   ① 드라이버 로딩/DB 접속 정보(URL·계정)   ② SQL 작성   ③ 파라미터 바인딩
 *   ④ 실행                                  ⑤ 자원 해제(close)   ⑥ 예외 처리
 *
 * 왜 문제인가 (PART 1의 SOLID 위반):
 *   - SRP 위반: 한 메서드가 '연결 만들기'와 '쿼리 실행' 두 가지 이유로 바뀐다.
 *   - OCP/DIP 위반: DB가 바뀌면(접속 정보·드라이버) add, get ... '모든 메서드'를 고쳐야 한다.
 *     변경 1번 = 수정 N곳 = 유지보수 지옥. 또 구체 클래스(DriverManager)에 직접 의존한다.
 *   - 중복: getConnection에 해당하는 코드가 add/get마다 그대로 복붙된다(아래에서 눈으로 확인).
 *
 * 다음 단계 예고: stage2에서 이 중복/혼재를 '관심사 분리'로 걷어낸다.
 *
 * (실행 가능하게 하려고 실제 H2 인메모리 DB를 쓴다. URL의 DB_CLOSE_DELAY=-1은 JVM이 살아있는 동안
 *  인메모리 DB가 유지되게 한다 — 안 그러면 연결이 닫힐 때 데이터가 사라진다.)
 */
public class UserDao {

    static final String URL = "jdbc:h2:mem:stage1;DB_CLOSE_DELAY=-1";
    static final String USER = "sa";
    static final String PW = "";

    public void add(User user) throws ClassNotFoundException, SQLException {
        // ① 드라이버 로딩 + ② 접속 정보 — DB가 바뀌면 여기를 고쳐야 한다(그리고 get에도 똑같이 있다!)
        Class.forName("org.h2.Driver");
        Connection c = DriverManager.getConnection(URL, USER, PW);

        // ③ SQL + ④ 바인딩 + ⑤ 실행
        PreparedStatement ps = c.prepareStatement("insert into users(id, name) values (?, ?)");
        ps.setString(1, user.getId());
        ps.setString(2, user.getName());
        ps.executeUpdate();

        // ⑥ 자원 해제 — 빠뜨리면 커넥션 누수. 예외까지 고려하면 더 복잡해진다(stage들에서 점차 개선).
        ps.close();
        c.close();
    }

    public User get(String id) throws ClassNotFoundException, SQLException {
        // ★ 주목: 아래 두 줄(드라이버 로딩 + 접속)은 add()와 '완전히 똑같다'. 이 중복이 핵심 고통이다.
        Class.forName("org.h2.Driver");
        Connection c = DriverManager.getConnection(URL, USER, PW);

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

    // 데모용: 테이블 생성(원래 DAO 책임은 아니지만 실행 가능하게 하려고 둠)
    public void createTable() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        try (Connection c = DriverManager.getConnection(URL, USER, PW)) {
            c.prepareStatement("create table if not exists users(id varchar(50) primary key, name varchar(50))")
                    .executeUpdate();
        }
    }
}
