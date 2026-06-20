package com.study.part12_aop.s10_advice_types;

/**
 * [12.10] 타겟 — order는 정상 반환, fail은 RuntimeException을 던진다(@AfterThrowing/@AfterReturning 구분용).
 */
public class RealOrderService implements OrderService {

    @Override
    public String order(String item) {
        System.out.println("    [비즈니스] " + item + " 주문 처리");
        return item + " 주문완료";
    }

    @Override
    public String fail(String item) {
        System.out.println("    [비즈니스] " + item + " 처리 중 오류 발생");
        throw new RuntimeException(item + " 재고 부족");
    }
}
