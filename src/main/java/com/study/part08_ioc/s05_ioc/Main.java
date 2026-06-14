package com.study.part08_ioc.s05_ioc;

import com.study.part08_ioc.domain.User;
import com.study.part08_ioc.s04_strategy.UserDao;

/**
 * [8.5 데모] IoC(제어의 역전) — 조립의 '제어권'이 Main에서 팩토리로 넘어갔다.
 *
 * 8.4 Main과 비교:
 *   - 8.4: Main이 'new NConnectionMaker()' + 'new UserDao(cm)'를 직접 했다(Main이 결정·생성).
 *   - 8.5: Main은 무엇을 new 할지 모른다. factory.userDao()로 '완성된 객체'를 받기만 한다(팩토리가 결정·생성).
 * → "어떤 구현을 쓸지"의 제어권이 내 코드(Main) -> 외부(DaoFactory)로 역전됐다 = IoC.
 *
 * 프레임워크 vs 라이브러리(개념):
 *   - 라이브러리: '내 코드'가 흐름을 쥐고 필요할 때 라이브러리를 '호출'한다(내 코드 -> 라이브러리).
 *   - 프레임워크: '프레임워크'가 흐름을 쥐고 '내 코드'를 '호출'한다(프레임워크 -> 내 코드). = IoC.
 *   - Hollywood Principle: "Don't call us, we'll call you." 8.6의 Spring이 바로 이 프레임워크다.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        DaoFactory factory = new DaoFactory();

        // Main은 ConnectionMaker가 N인지 D인지 모른다. 그냥 '완성된 UserDao'를 받는다(제어권이 팩토리에).
        UserDao dao = factory.userDao();

        dao.createTable();
        dao.add(new User("s5", "IoC출발"));
        System.out.println("[8.5] " + dao.get("s5"));
        System.out.println("=> Main은 무엇을 new 할지 모르고 factory.userDao()로 완성품을 받는다.");
        System.out.println("   '어떤 구현을 쓸지'의 결정권이 Main -> 팩토리(외부)로 넘어감 = IoC(제어의 역전).");
        System.out.println("   아직은 '평범한 자바 팩토리'다. -> 8.6: 여기에 @Configuration/@Bean을 붙여 Spring 컨테이너가 대신.");
    }
}
