package com.study.part08_ioc.s06_appcontext_di;

import com.study.part08_ioc.domain.User;
import com.study.part08_ioc.s04_strategy.UserDao;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * [8.6 데모] IoC 컨테이너(ApplicationContext)로 객체를 얻는다 — Spring 본격 진입.
 *
 * 8.5와 비교: 8.5는 'new DaoFactory().userDao()'로 내가 팩토리를 직접 호출했다. 8.6은 그 조립을
 * Spring 컨테이너(ApplicationContext)가 대신 수행하고, 우리는 getBean으로 '완성된 빈'을 꺼내 쓰기만 한다.
 *
 * 확인 포인트:
 *   1) getBean으로 UserDao를 꺼내 add/get이 동작한다(컨테이너가 의존성까지 주입해 완성해 줌 = DI).
 *   2) getBean을 여러 번 호출해도 '같은 인스턴스'다(싱글톤 레지스트리). dao1 == dao2 가 true.
 *
 * 용어:
 *   - 빈(Bean): Spring 컨테이너가 만들고 관리하는 객체.
 *   - BeanFactory(기본) -> ApplicationContext(확장: 국제화·이벤트 등 + 실무 표준).
 *   - getBean(): 빈 목록에서 찾고, 없으면 @Bean 메서드를 호출해 생성·주입한 뒤 돌려준다.
 */
public class Main {
    public static void main(String[] args) throws Exception {
        // 1) 설정(DaoFactory)을 읽어 컨테이너 생성. 이 순간 컨테이너가 빈을 만들고 의존관계를 주입한다.
        ApplicationContext ctx = new AnnotationConfigApplicationContext(DaoFactory.class);

        // 2) 완성된 빈을 꺼낸다(우리가 new 하지 않는다 — 컨테이너가 만들어 줌).
        UserDao dao = ctx.getBean("userDao", UserDao.class);
        dao.createTable();
        dao.add(new User("s6", "ApplicationContext"));
        System.out.println("[8.6] " + dao.get("s6"));

        // 3) 싱글톤 확인: 몇 번을 꺼내도 같은 객체.
        UserDao dao1 = ctx.getBean("userDao", UserDao.class);
        UserDao dao2 = ctx.getBean("userDao", UserDao.class);
        System.out.println("[8.6] dao1 == dao2 ? " + (dao1 == dao2) + "  (true = 싱글톤 레지스트리)");

        System.out.println("=> 객체 생성·주입(조립)을 컨테이너가 대신한다(IoC/DI). getBean은 항상 같은 싱글톤 빈을 준다.");
        System.out.println("   주의: 싱글톤 빈은 stateless여야 안전(인스턴스 변수에 요청별 데이터 보관 금지 — PART 7 동시성).");

        ((AnnotationConfigApplicationContext) ctx).close();
    }
}
