package com.study.part08_ioc.stage5_ioc_container;

import com.study.part08_ioc.domain.User;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * [STAGE 5 데모 / 8.5 + 8.6] IoC 컨테이너(ApplicationContext)로 객체를 얻는다.
 *
 * stage4와 비교: stage4는 'new UserDao(new NConnectionMaker())'를 Main이 직접 조립했다. stage5는 그
 * 조립을 DaoFactory에 적어두고, ApplicationContext(컨테이너)가 대신 생성·주입한다. 우리는 getBean으로
 * '완성된 객체'를 꺼내 쓰기만 한다 = IoC(제어의 역전).
 *
 * 확인 포인트:
 *   1) getBean으로 UserDao를 꺼내 add/get이 동작한다(컨테이너가 의존성까지 주입해 완성해줌 = DI).
 *   2) getBean을 여러 번 호출해도 '같은 인스턴스'다(싱글톤 레지스트리). dao1 == dao2 가 true.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        // 1) 설정(DaoFactory)을 읽어 컨테이너 생성. 이 순간 컨테이너가 빈들을 만들고 의존관계를 주입한다.
        ApplicationContext ctx = new AnnotationConfigApplicationContext(DaoFactory.class);

        // 2) 완성된 빈을 꺼낸다(우리가 new 하지 않는다 — 컨테이너가 만들어 줌).
        UserDao dao = ctx.getBean("userDao", UserDao.class);
        dao.createTable();
        dao.add(new User("s5", "IoC완성"));
        System.out.println("[stage5] " + dao.get("s5"));

        // 3) 싱글톤 확인: 몇 번을 꺼내도 같은 객체.
        UserDao dao1 = ctx.getBean("userDao", UserDao.class);
        UserDao dao2 = ctx.getBean("userDao", UserDao.class);
        System.out.println("[stage5] dao1 == dao2 ? " + (dao1 == dao2) + "  (true = 싱글톤 레지스트리)");

        System.out.println("=> 객체 생성·주입(조립)을 컨테이너가 대신한다(IoC). getBean은 항상 같은 싱글톤 빈을 준다.");
        System.out.println("   주의: 싱글톤 빈은 stateless여야 안전(인스턴스 변수에 요청별 데이터 보관 금지 — PART 7 동시성).");

        ((AnnotationConfigApplicationContext) ctx).close();
    }
}
