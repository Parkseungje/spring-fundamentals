package com.study.part08_ioc.s03_design_pattern;

import com.study.part08_ioc.s02_separation.AbstractUserDao;

/**
 * [8.3] 상속 기반 분리의 한계 ① — 단일 상속 제약을 코드로 드러낸다.
 *
 * 8.2의 구조는 'AbstractUserDao를 상속'해서 연결 방법을 갈아끼웠다. 그런데 자바 클래스는 부모를 '하나만'
 * 상속할 수 있다. 만약 어떤 UserDao 구현이 다른 클래스(예: 공통 기능을 담은 BaseComponent)도 상속해야
 * 한다면? AbstractUserDao와 둘 다는 불가능하다.
 *
 * 아래 BaseComponent를 상속한 클래스는 AbstractUserDao를 '또' 상속할 수 없다(주석의 컴파일 에러 참고).
 * 즉 "연결 분리"를 위해 단 하나뿐인 상속 카드를 AbstractUserDao에 써버린 것이 한계다.
 * -> 8.4에서 상속 대신 '인터페이스 구현 + 합성'으로 바꿔 이 제약을 푼다(인터페이스는 여러 개 구현 가능).
 */
public class SingleInheritanceLimitation {

    // 어떤 공통 기능을 담은 부모가 이미 있다고 하자.
    static class BaseComponent {
        void init() { System.out.println("  BaseComponent.init()"); }
    }

    // 이 클래스는 BaseComponent를 상속했다. 그래서 AbstractUserDao를 '또' 상속할 수 없다.
    //   class MyDao extends BaseComponent, AbstractUserDao { }  // ❌ 컴파일 에러: 다중 상속 불가
    // 결국 BaseComponent를 포기하거나 AbstractUserDao를 포기해야 한다 — 이것이 단일 상속의 한계.
    static class MyComponent extends BaseComponent {
        // AbstractUserDao의 add/get을 쓰고 싶어도, 이미 BaseComponent를 상속해 불가능하다.
    }

    public static void main(String[] args) {
        System.out.println("[8.3 한계①] 단일 상속 제약");
        new MyComponent().init();
        System.out.println("  MyComponent는 BaseComponent를 상속함 -> AbstractUserDao를 '또' 상속 불가(자바 단일 상속).");
        System.out.println("  즉 '연결 분리'를 위해 유일한 상속 카드를 " + AbstractUserDao.class.getSimpleName()
                + "에 써버린 셈. -> 8.4: 인터페이스+합성으로 해결.");
    }
}
