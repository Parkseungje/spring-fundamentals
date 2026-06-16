package com.study.part12_aop.s03_pattern_evolution;

/**
 * [12.3] 세 패턴이 공통으로 끼워 넣으려는 '변하지 않는 부가 코드' — 로그 추적(시작/종료/시간/예외).
 *
 * 12.1에서 본 흩어진 관심사(로깅)를 여기서는 '디자인 패턴'으로 한곳에 모으려 한다. 이 클래스는 그
 * 부가 기능 자체를 담은 헬퍼다. 핵심은 begin~end가 '변하지 않는 흐름'이고, 그 사이에 들어갈 비즈니스
 * 로직만 '변하는 부분'이라는 것 — 이 둘을 어떻게 분리하느냐가 템플릿 메서드/전략/템플릿 콜백의 차이다.
 */
public class Trace {

    public static class Status {
        final String message;
        final long startMillis;
        Status(String message, long startMillis) {
            this.message = message;
            this.startMillis = startMillis;
        }
    }

    public Status begin(String message) {
        System.out.println("--> [" + message + "] 시작");
        return new Status(message, System.currentTimeMillis());
    }

    public void end(Status status) {
        long took = System.currentTimeMillis() - status.startMillis;
        System.out.println("<-- [" + status.message + "] 종료 (" + took + "ms)");
    }

    public void exception(Status status, Exception e) {
        long took = System.currentTimeMillis() - status.startMillis;
        System.out.println("<X- [" + status.message + "] 예외 " + e.getMessage() + " (" + took + "ms)");
    }
}
