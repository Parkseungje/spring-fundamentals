package com.study.part12_aop.s04_proxy;

/**
 * [12.4] 프록시 성립의 핵심 조건 1 — 클라이언트가 의존할 '인터페이스'.
 *
 * 프록시(대리인)가 성립하려면 "클라이언트가 진짜 객체인지 프록시인지 몰라야" 한다. 그러려면 진짜 객체
 * (RealOrderService)와 프록시(LogProxy/CachingProxy)가 '같은 인터페이스'를 구현해야 하고, 클라이언트는
 * 구체 클래스가 아니라 이 인터페이스에만 의존해야 한다(PART 8.4 DIP). 그래야 무엇을 주입하든 클라이언트
 * 코드는 0줄 바뀐다.
 *
 * 메서드를 둘 둔 이유:
 *  - order(): '부가 기능' 프록시(로그/시간 측정) 데모용.
 *  - findStock(): '접근 제어' 프록시(캐싱) 데모용 — 일부러 비싼(느린) 조회로 가정한다.
 */
public interface OrderService {

    // 주문 처리(쓰기) — 부가 기능(로깅) 프록시로 감쌀 대상
    String order(String item);

    // 재고 조회(읽기) — 비싸다고 가정. 접근 제어(캐싱) 프록시로 호출 자체를 줄일 대상
    int findStock(String item);
}
