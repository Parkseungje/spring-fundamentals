package com.study.part12_aop.s04_proxy;

/**
 * [12.4] 로그 추적 헬퍼 — 프록시가 더하는 '부가 기능'의 내용물(begin/end/시간/예외).
 * (s03의 Trace와 동일한 역할이지만, 소단원별 패키지를 독립적으로 두기 위해 같은 패키지에 둔다.)
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
