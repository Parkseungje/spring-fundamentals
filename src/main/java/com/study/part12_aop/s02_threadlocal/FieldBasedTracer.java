package com.study.part12_aop.s02_threadlocal;

/**
 * [12.2] '인스턴스 필드'에 상태를 담는 로그 추적기 — 싱글톤 + 멀티스레드에서 깨지는 버전.
 *
 * 12.1의 LogTracer는 호출마다 TraceStatus를 새로 만들어 넘겼다(상태를 인자로 전달). 그런데 실무에선
 * "지금 처리 중인 요청의 트랜잭션 ID와 호출 깊이"를 메서드 사이에서 공유하고 싶다(begin에서 깊이 +1,
 * 중첩 호출도 같은 traceId로 묶기). 그래서 그 상태를 '필드'에 들고 있게 만들면 편해 보인다.
 *
 * 문제: 이 Tracer는 Spring 빈으로 쓰면 '싱글톤'(PART 8)이라 인스턴스가 딱 하나다. 그 하나를 여러 요청
 * (여러 스레드, PART 7)이 동시에 쓰면, traceId/depth 필드를 서로 덮어써 로그가 뒤섞인다.
 * 즉 '상태를 가진 싱글톤'은 스레드 안전하지 않다(PART 8.5에서 본 '싱글톤은 무상태여야 한다'의 반례).
 *
 * 이 클래스는 그 문제를 '재현'하기 위한 것이다. 해결은 ThreadLocalTracer(다음 파일).
 */
public class FieldBasedTracer {

    // 공유되는 가변 상태 — 싱글톤 인스턴스 하나에 단 한 벌만 존재한다.
    // 여러 스레드가 동시에 begin/log를 호출하면 이 두 필드를 서로 덮어쓴다(경쟁 상태, PART 7.4).
    private String traceId;   // 이 요청을 식별하는 ID
    private int depth;        // 호출 깊이(중첩 호출마다 +1)

    public void begin(String requestName) {
        // 새 요청 시작: traceId를 만들고 깊이를 0부터 시작한다고 '가정'한다.
        // 하지만 필드는 모든 스레드가 공유하므로, 다른 스레드가 방금 set한 값을 덮어쓰거나 읽게 된다.
        this.traceId = requestName + "-" + (int) (Math.random() * 1000);
        this.depth = 0;
        log("begin");
    }

    public void log(String message) {
        // traceId/depth를 '필드'에서 읽는다 → 다른 스레드가 바꿔놓은 값을 볼 수 있다(섞임).
        String indent = "  ".repeat(Math.max(depth, 0));
        System.out.println("[" + Thread.currentThread().getName() + "] traceId=" + traceId
                + " " + indent + "|" + message);
        this.depth++;
    }
}
