package com.study.part12_aop.s06_dynamic_proxy;

/**
 * [12.6] 진짜 객체 — 순수 비즈니스. 동적 프록시가 이 객체를 감싼다(원본 불변).
 */
public class RealOrderService implements OrderService {

    @Override
    public String order(String item) {
        System.out.println("    [비즈니스] " + item + " 주문 처리");
        return item + " 주문완료";
    }
}
