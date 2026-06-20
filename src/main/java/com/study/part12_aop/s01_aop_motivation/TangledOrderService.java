package com.study.part12_aop.s01_aop_motivation;

import com.study.part12_aop.s01_aop_motivation.LogTracer.TraceStatus;

/**
 * [12.1] '엉킴(tangling)'을 극대화해 보여주는 서비스 — 한 메서드에 부가 관심사가 여러 개 겹친 버전.
 *
 * OrderService는 '로깅' 하나만 흩어진 모습이었다. 그런데 실무의 한 메서드에는 보통 로깅뿐 아니라
 * 보안(권한 검사)·트랜잭션(시작/커밋/롤백) 같은 부가 관심사가 '동시에' 박힌다. 그러면:
 *   - Tangling(엉킴): 한 메서드 '안'에서 핵심(비즈니스)과 부가(로그+보안+트랜잭션)가 뒤엉킨다.
 *   - Scattering(흩어짐): 이 엉킨 덩어리가 모든 메서드에 또 반복된다.
 *
 * 아래 placeOrder()를 보면 '진짜 비즈니스'는 가운데 두 줄뿐인데, 그 위아래를 보안 검사 + 트랜잭션
 * 관리 + 로그 추적이 둘러싸 비즈니스가 파묻혀 버린다. 관심사가 늘수록 엉킴·흩어짐이 곱으로 악화된다.
 * 이 가중된 고통이 AOP(부가 관심사를 한 곳에 모아 자동으로 끼워 넣기)의 더 강한 동기다.
 *
 * (여기서 보안/트랜잭션은 개념을 보여주기 위한 가짜 구현이다 — 실제 트랜잭션은 PART 13, 보안은 PART 19.)
 */
public class TangledOrderService {

    private final LogTracer tracer;

    public TangledOrderService(LogTracer tracer) {
        this.tracer = tracer;
    }

    public void placeOrder(String user, String item) {
        // 부가 관심사 1) 로깅 시작
        TraceStatus status = tracer.begin("TangledOrderService.placeOrder");
        // 부가 관심사 2) 보안(권한 검사)
        if (user == null || !user.equals("admin")) {
            tracer.exception(status, new SecurityException("권한 없음"));
            throw new SecurityException("권한 없음: " + user);
        }
        // 부가 관심사 3) 트랜잭션 시작
        System.out.println("    [트랜잭션] begin");
        try {
            // ===== 진짜 비즈니스 로직(핵심)은 '이 두 줄'뿐 =====
            System.out.println("    [비즈니스] " + item + " 재고 차감");
            System.out.println("    [비즈니스] " + item + " 주문 생성");
            // ================================================
            System.out.println("    [트랜잭션] commit");   // 부가 3) 커밋
            tracer.end(status);                            // 부가 1) 로깅 종료
        } catch (Exception e) {
            System.out.println("    [트랜잭션] rollback");  // 부가 3) 롤백
            tracer.exception(status, e);                   // 부가 1) 예외 로그
            throw e;
        }
    }
}
