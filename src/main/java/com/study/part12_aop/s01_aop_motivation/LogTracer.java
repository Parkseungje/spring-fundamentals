package com.study.part12_aop.s01_aop_motivation;

/**
 * [12.1] 로그 추적기 — 메서드 호출 시작/종료와 실행 시간을 찍는 '부가 기능'.
 *
 * 이건 비즈니스 로직(핵심 관심사)이 아니라, 여러 메서드에 공통으로 필요한 '부가 관심사'다(로깅·시간 측정).
 * 문제는 이 부가 기능을 넣으려면 '모든 메서드'에 begin/end/exception 호출과 try-catch를 박아야 한다는 것.
 * 그 고통을 다음 클래스들(OrderService 등)에서 직접 보게 된다.
 *
 * (여기선 단순화를 위해 호출 깊이·동시성을 고려하지 않는다 — ThreadLocal로 깊이/동시성을 다루는 건 12.2.)
 */
public class LogTracer {

    public static class TraceStatus {
        final String message;
        final long startMillis;
        TraceStatus(String message, long startMillis) {
            this.message = message;
            this.startMillis = startMillis;
        }
    }

    public TraceStatus begin(String message) {
        System.out.println("--> [" + message + "] 시작");
        return new TraceStatus(message, System.currentTimeMillis());
    }

    public void end(TraceStatus status) {
        long took = System.currentTimeMillis() - status.startMillis;
        System.out.println("<-- [" + status.message + "] 종료 (" + took + "ms)");
    }

    public void exception(TraceStatus status, Exception e) {
        long took = System.currentTimeMillis() - status.startMillis;
        System.out.println("<X- [" + status.message + "] 예외 " + e.getMessage() + " (" + took + "ms)");
    }
}
