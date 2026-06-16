package com.study.part12_aop.s04_proxy;

import java.util.HashMap;
import java.util.Map;

/**
 * [12.4] '접근 제어' 프록시 — 캐싱으로 진짜 객체 호출 자체를 줄인다(부가 기능과의 대비).
 *
 * 프록시의 두 기능 중 '접근 제어(캐싱/권한/지연 로딩)'에 해당한다. LogProxy와 결정적으로 다른 점:
 *  - LogProxy: 항상 진짜 객체를 부른다(앞뒤로 로그만 더함).
 *  - CachingProxy: 캐시에 값이 있으면 '진짜 객체를 아예 안 부른다'(=접근을 제어/차단). 호출 흐름 자체를 바꾼다.
 *
 * 여기선 findStock 결과를 캐시한다. 첫 호출은 진짜 객체로 내려가 느리지만(200ms), 두 번째 같은 인자
 * 호출은 캐시에서 즉시 반환한다(진짜 조회 생략). order는 캐싱 대상이 아니라 그대로 위임한다.
 * 권한 체크 프록시도 같은 부류다(권한 없으면 진짜 호출을 막고 예외) — '접근을 제어'한다는 점이 공통.
 */
public class CachingProxy implements OrderService {

    private final OrderService target;
    private final Map<String, Integer> stockCache = new HashMap<>();

    public CachingProxy(OrderService target) {
        this.target = target;
    }

    @Override
    public String order(String item) {
        return target.order(item); // 캐싱 대상 아님 -> 그대로 위임
    }

    @Override
    public int findStock(String item) {
        // 접근 제어의 핵심: 캐시에 있으면 진짜 객체를 '부르지 않는다'.
        if (stockCache.containsKey(item)) {
            System.out.println("    [캐시] " + item + " 재고를 캐시에서 즉시 반환(진짜 조회 생략)");
            return stockCache.get(item);
        }
        int stock = target.findStock(item); // 캐시 미스일 때만 진짜 호출
        stockCache.put(item, stock);
        return stock;
    }
}
