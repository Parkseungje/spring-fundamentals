package com.study.part08_ioc.stage3_template_method;

import com.study.part08_ioc.domain.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * [STAGE 3 / 8.2-2단계 + 8.3] 관심사의 분리 — 2단계: 추상클래스 + 템플릿 메소드 패턴.
 *
 * 요구사항 진화: "고객사마다 DB가 다르다. 그런데 UserDao의 핵심 코드(add/get 흐름)는 공개/수정하지 않고,
 * 'DB 연결 방식'만 고객사가 갈아끼우게 하고 싶다." stage2의 private getConnection으로는 이게 안 된다
 * (UserDao를 직접 고쳐야 하므로).
 *
 * 해결: getConnection()을 '추상 메서드'로 만든다. 즉
 *   - 변하지 않는 흐름(add/get의 SQL 실행 절차) = 부모(UserDao)에 고정.
 *   - 변하는 부분(연결 만드는 방법)          = 자식(NUserDao/DUserDao)이 구현.
 *
 * 적용된 두 디자인 패턴 (8.3):
 *   - 템플릿 메소드 패턴: 슈퍼클래스가 '뼈대(흐름)'를 정하고, 변하는 한 부분만 서브클래스가 채운다.
 *     (Spring JdbcTemplate의 'Template'이 이 이름에서 왔다.)
 *   - 팩토리 메소드 패턴: 객체 생성(Connection)을 서브클래스에 위임한다.
 *     (Spring BeanFactory의 'Factory'가 이 개념.)
 *
 * 그래도 남는 '상속 기반'의 한계 (-> stage4에서 합성으로 전환):
 *   - 자바는 단일 상속: UserDao를 상속해버리면 자식은 다른 부모를 못 가진다.
 *   - 컴파일 타임 결합: "어떤 연결 방식을 쓸지"가 '어떤 자식 클래스를 new 하느냐'로 코드에 박힌다.
 *   - 부모-자식 강결합: 부모(UserDao)가 바뀌면 모든 자식이 영향받는다.
 *
 * 그래서 UserDao는 'abstract'다 — 연결 방법을 모르므로 혼자선 객체가 될 수 없고, 자식을 통해서만 쓰인다.
 */
public abstract class UserDao {

    // 변하지 않는 흐름(템플릿): 연결을 '어떻게' 얻는지는 모른 채, 얻은 연결로 SQL을 실행한다.
    public void add(User user) throws ClassNotFoundException, SQLException {
        Connection c = getConnection();   // ← 실제 구현은 자식이 결정(추상 메서드 호출)
        PreparedStatement ps = c.prepareStatement("insert into users(id, name) values (?, ?)");
        ps.setString(1, user.getId());
        ps.setString(2, user.getName());
        ps.executeUpdate();
        ps.close();
        c.close();
    }

    public User get(String id) throws ClassNotFoundException, SQLException {
        Connection c = getConnection();
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
        try (Connection c = getConnection()) {
            c.prepareStatement("create table if not exists users(id varchar(50) primary key, name varchar(50))")
                    .executeUpdate();
        }
    }

    // ★ 변하는 부분 = 추상 메서드. "연결은 너(자식)가 만들어와" — 흐름은 부모, 생성은 자식(팩토리 메소드).
    protected abstract Connection getConnection() throws ClassNotFoundException, SQLException;
}
