package com.study.part08_ioc.s01_traditional;

import com.study.part08_ioc.domain.User;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * [8.1] 전통 DAO의 문제 — 한 메서드가 모든 책임을 떠안은 '로우레벨' 코드.
 *
 * 이 소단원이 보여주려는 것: "동작은 하지만 유지보수가 지옥인" 코드의 실체.
 * add()/get() 한 메서드 안에 아래 책임이 전부 뒤엉켜 있다.
 *   ① 드라이버 로딩 / DB 접속 정보(URL·계정)   ② SQL 작성   ③ 파라미터 바인딩
 *   ④ 실행                                      ⑤ 자원 해제(close)   ⑥ 예외 처리
 *
 * 왜 문제인가 (PART 1의 SOLID 위반):
 *   - SRP 위반: 한 메서드가 '연결 만들기'와 '쿼리 실행'이라는 서로 다른 이유로 바뀐다.
 *   - OCP/DIP 위반: DB가 바뀌면(접속 정보·드라이버) add, get ... '모든 메서드'를 고쳐야 한다.
 *     변경 1번 = 수정 N곳 = 유지보수 지옥. 게다가 구체 클래스(DriverManager)에 직접 의존한다.
 *   - 중복: '드라이버 로딩 + 접속' 코드가 add/get에 그대로 복붙된다(아래에서 눈으로 확인).
 *
 * 다음 소단원 예고: 8.2에서 이 중복/혼재를 '관심사의 분리'로 걷어낸다.
 *
 * (실행 가능하도록 실제 H2 인메모리 DB를 쓴다. URL의 DB_CLOSE_DELAY=-1은 JVM이 살아있는 동안 인메모리
 *  DB를 유지시킨다 — 안 그러면 마지막 연결이 닫힐 때 데이터가 사라진다.)
 */
public class UserDao {

    static final String URL = "jdbc:h2:mem:part08_s01;DB_CLOSE_DELAY=-1";
    static final String DB_USER = "sa";
    static final String DB_PW = "";

    public void add(User user) throws ClassNotFoundException, SQLException {
        // ① 드라이버 로딩 + ② 접속 정보 — DB가 바뀌면 여기를 고쳐야 한다(그리고 get()에도 똑같이 있다!)
        Class.forName("org.h2.Driver");
        Connection c = DriverManager.getConnection(URL, DB_USER, DB_PW);

        // ③ SQL + ④ 바인딩 + ⑤ 실행
        PreparedStatement ps = c.prepareStatement("insert into users(id, name) values (?, ?)");
        ps.setString(1, user.getId());
        ps.setString(2, user.getName());
        ps.executeUpdate();

        // ⑥ 자원 해제 — 빠뜨리면 커넥션 누수. 예외까지 고려하면 더 복잡해진다(이후 소단원에서 점차 개선).
        ps.close();
        c.close();
    }

    public User get(String id) throws ClassNotFoundException, SQLException {
        // ★ 주목: 아래 두 줄(드라이버 로딩 + 접속)은 add()와 '완전히 똑같다'. 이 중복이 8.1의 핵심 고통이다.
        Class.forName("org.h2.Driver");
        Connection c = DriverManager.getConnection(URL, DB_USER, DB_PW);

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

    // 데모용: 테이블 생성(원래 DAO만의 책임은 아니지만 실행 가능하게 하려고 둔다)
    public void createTable() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        try (Connection c = DriverManager.getConnection(URL, DB_USER, DB_PW)) {
            c.prepareStatement("create table if not exists users(id varchar(50) primary key, name varchar(50))")
                    .executeUpdate();
        }
    }
}
