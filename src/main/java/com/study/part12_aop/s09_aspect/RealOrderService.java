package com.study.part12_aop.s09_aspect;

/**
 * [12.9] 타겟(Target) — 부가 기능이 적용되는 실제 객체. 순수 비즈니스.
 */
public class RealOrderService implements OrderService {

    @Override
    public String order(String item) {
        System.out.println("    [비즈니스] " + item + " 주문 처리");
        return item + " 주문완료";
    }

    @Override
    public int findStock(String item) {
        System.out.println("    [비즈니스] " + item + " 재고 조회");
        return 10;
    }
}
