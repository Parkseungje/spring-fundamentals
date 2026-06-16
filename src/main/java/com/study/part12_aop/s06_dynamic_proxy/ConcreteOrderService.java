package com.study.part12_aop.s06_dynamic_proxy;

/**
 * [12.6] 인터페이스가 '없는' 구체 클래스 — CGLIB 대상.
 *
 * JDK 동적 프록시는 인터페이스가 있어야만 쓸 수 있다. 그런데 현실엔 인터페이스 없이 구체 클래스만 있는
 * 경우가 많다(컨트롤러·서비스 등). 이럴 때 CGLIB가 '클래스를 상속'해 프록시를 만든다(예제3).
 *
 * pay()를 'final'로 둔 이유: CGLIB는 상속+오버라이드로 동작하므로 'final 메서드는 프록시할 수 없다'는
 * 한계를 보여주기 위함이다(final은 오버라이드 불가). 이 점이 PART 14 JPA에서 '엔티티에 final 금지'와 직결된다.
 */
public class ConcreteOrderService {

    public String order(String item) {
        System.out.println("    [비즈니스] " + item + " 주문 처리(구체 클래스)");
        return item + " 주문완료";
    }

    // final 메서드: CGLIB가 오버라이드할 수 없어 '프록시(부가 기능)가 적용되지 않는다'.
    public final String pay(String item) {
        System.out.println("    [비즈니스] " + item + " 결제 처리(final 메서드)");
        return item + " 결제완료";
    }
}
