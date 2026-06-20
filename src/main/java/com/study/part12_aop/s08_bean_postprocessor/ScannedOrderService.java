package com.study.part12_aop.s08_bean_postprocessor;

import org.springframework.stereotype.Service;

/**
 * [12.8] 컴포넌트 스캔으로 등록되는 빈(@Service). 자동 프록시 생성기가 이런 '스캔된 실제 객체'도 프록시로 바꾼다.
 *
 * 12.7의 한계 중 하나: @Service/@Repository로 컴포넌트 스캔된 빈은 우리가 ProxyFactory로 바꿔치기할 틈이
 * 없다(컨테이너가 알아서 만들어 등록하므로). 자동 프록시 생성기는 그 등록 과정(빈 후처리)에 끼어들어 스캔된
 * 빈도 프록시로 교체할 수 있다 -> Example3에서 이를 확인한다.
 */
@Service
public class ScannedOrderService implements OrderService {

    @Override
    public String order(String item) {
        System.out.println("    [비즈니스] (스캔된 빈) " + item + " 주문 처리");
        return item + " 주문완료";
    }

    @Override
    public int findStock(String item) {
        System.out.println("    [비즈니스] (스캔된 빈) " + item + " 재고 조회");
        return 10;
    }
}
