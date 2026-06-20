package com.study.part12_aop.s08_bean_postprocessor;

/**
 * [12.8] 진짜 객체(@Bean으로 등록). 순수 비즈니스. 빈 후처리기가 이걸 프록시로 바꿔치기한다.
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
