package com.study.part08_ioc.stage4_strategy;

import com.study.part08_ioc.domain.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * [STAGE 4 / 8.4] 인터페이스 + 합성 → OCP + 전략 패턴 (DIP의 실현).
 *
 * stage3(상속)의 한계를 푼다. UserDao는 더 이상 abstract도 아니고 자식도 없다. 대신:
 *   - ConnectionMaker '인터페이스'에만 의존한다(구체 구현은 모름 = DIP).
 *   - 실제 구현체를 '생성자로 외부에서 주입'받아 필드로 들고 쓴다(합성, composition).
 *
 * 무엇이 좋아졌나:
 *   - OCP: 새 DB(연결 방식)가 생겨도 ConnectionMaker 구현체만 추가하면 되고 UserDao는 '수정 0'.
 *   - 단일 상속 제약 해방: UserDao는 아무것도 상속하지 않으니 자유롭다.
 *   - 결합이 런타임으로: "어떤 구현을 쓸지"가 코드에 안 박히고, 주입하는 외부가 정한다.
 *
 * 전략 패턴 용어 매핑: Context=UserDao, Strategy=ConnectionMaker, ConcreteStrategy=N/DConnectionMaker.
 * → 전략 패턴은 OCP를 구현하는 도구이고, 여기서 PART 1의 DIP("구체가 아니라 추상에 의존")가 실현된다.
 *
 * ★ 그런데 아직 '한 가지'가 남았다: 그럼 'new NConnectionMaker()'를 도대체 누가 하는가? 그걸 만들어
 *   UserDao 생성자에 넣어주는 책임은 아직 클라이언트(Main)에 있다. 이 '생성·연결' 책임을 외부(컨테이너)로
 *   넘기는 것이 stage5의 IoC다.
 */
public class UserDao {

    private final ConnectionMaker connectionMaker;   // 인터페이스에만 의존 (구체 구현 모름)

    // ★ 생성자 주입: 어떤 ConnectionMaker를 쓸지 '외부'가 정해서 넣어준다(UserDao가 직접 new 하지 않음).
    public UserDao(ConnectionMaker connectionMaker) {
        this.connectionMaker = connectionMaker;
    }

    public void add(User user) throws ClassNotFoundException, SQLException {
        Connection c = connectionMaker.makeConnection();   // 주입받은 전략에 위임
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
