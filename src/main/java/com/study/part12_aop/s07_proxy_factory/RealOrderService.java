package com.study.part12_aop.s07_proxy_factory;

/**
 * [12.7] 진짜 객체(인터페이스 구현) — ProxyFactory가 감쌀 대상. 순수 비즈니스만.
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
