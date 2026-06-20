package com.study.part12_aop.s10_advice_types;

import org.aspectj.lang.annotation.Pointcut;

/**
 * [12.10] @Pointcut을 별도 클래스로 모아 재사용한다(DRY).
 *
 * 어드바이스마다 "execution(* order(..))"를 문자열로 반복하면, 대상이 바뀔 때 모든 곳을 고쳐야 한다(중복).
 * @Pointcut으로 '이름 붙인 시그니처'를 한 번 정의하면 여러 어드바이스가 그 이름을 참조해 재사용한다.
 * 별도 클래스로 모으면 여러 애스펙트가 공유할 수 있고, &&/||/! 로 조합도 가능하다.
 *
 * 다른 클래스에서 참조할 땐 '패키지.클래스.메서드()' 전체 경로로 쓴다.
 * 예: @Before("com.study.part12_aop.s10_advice_types.Pointcuts.orders()")
 */
public class Pointcuts {

    // 이름 붙인 포인트컷: order 메서드 (메서드 본문은 비움 — 시그니처 선언용)
    @Pointcut("execution(* order(..))")
    public void orders() {
    }

    // 이름 붙인 포인트컷: fail 메서드
    @Pointcut("execution(* fail(..))")
    public void fails() {
    }

    // 조합: order '이거나' fail (||). 정상/예외 메서드를 한 번에 가리킨다.
    @Pointcut("orders() || fails()")
    public void orderOrFail() {
    }

    // 조합: OrderService(및 구현체)의 모든 메서드 '이면서'(&&) fail은 '제외'(!) — &&/! 조합 예시.
    // (대상을 OrderService+ 로 좁힌다. 패키지 전체로 넓히면 애스펙트 자신까지 매칭돼 순환참조가 난다.)
    @Pointcut("execution(* com.study.part12_aop.s10_advice_types.OrderService+.*(..)) && !fails()")
    public void everythingButFail() {
    }
}
