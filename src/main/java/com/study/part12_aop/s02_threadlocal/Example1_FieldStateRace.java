package com.study.part12_aop.s02_threadlocal;

import java.util.concurrent.CountDownLatch;

/**
 * [12.2] 예제1 — '상태를 가진 싱글톤'을 여러 스레드가 동시에 쓰면 로그가 섞인다(문제 재현).
 *
 * 가설: FieldBasedTracer 인스턴스 '하나'(싱글톤 흉내)를 여러 스레드가 동시에 begin/log 하면,
 * traceId/depth 필드를 서로 덮어써 "A 스레드 로그에 B의 traceId가 찍히는" 식으로 뒤섞인다.
 *
 * 왜 이렇게 짰나: 일부러 Tracer를 '한 개'만 만들어 모든 스레드에 공유시킨다(빈=싱글톤의 재현).
 * 그리고 begin 직후 sleep을 넣어, 한 스레드가 begin으로 traceId를 set한 뒤 log하기 전에 다른 스레드가
 * 끼어들어 traceId를 덮어쓰도록 '경쟁을 강제'한다. 그래야 버그가 매번 또렷이 드러난다(PART 7.4 가시성/원자성).
 */
public class Example1_FieldStateRace {

    public static void main(String[] args) throws InterruptedException {
        // 싱글톤 흉내: Tracer는 단 하나, 모든 스레드가 이 하나를 공유한다.
        FieldBasedTracer tracer = new FieldBasedTracer();

        int threads = 3;
        CountDownLatch ready = new CountDownLatch(threads); // 모두 동시에 출발시키기 위한 신호
        CountDownLatch go = new CountDownLatch(1);

        System.out.println("== [문제] 상태를 필드에 둔 싱글톤 Tracer를 3개 스레드가 동시 사용 ==");
        for (int i = 1; i <= threads; i++) {
            String requestName = "요청" + i;
            new Thread(() -> {
                try {
                    ready.countDown();
                    go.await();                 // 모두 준비되면 일제히 출발(경쟁 극대화)
                    tracer.begin(requestName);  // traceId를 내 요청 이름으로 set
                    Thread.sleep(20);           // 여기서 다른 스레드가 끼어들어 traceId를 덮어쓴다
                    tracer.log("처리 중");       // 이때 내 traceId가 아니라 '남의 traceId'가 찍힐 수 있다
                } catch (InterruptedException ignored) {
                }
            }, requestName + "-스레드").start();
        }

        ready.await();
        go.countDown(); // 출발!
        Thread.sleep(500);

        System.out.println("\n=> 같은 스레드의 로그인데 traceId가 begin과 log에서 달라지거나, 서로의 요청 ID가");
        System.out.println("   섞여 찍힌다. 원인: traceId/depth가 싱글톤의 '공유 필드'라 스레드끼리 덮어쓰기 때문.");
        System.out.println("   (PART 8.5 '싱글톤은 무상태여야 한다'의 반례 + PART 7.4 경쟁 상태) -> 해결은 예제2.");
    }
}
