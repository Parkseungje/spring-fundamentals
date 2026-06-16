package com.study.part12_aop.s02_threadlocal;

import java.util.concurrent.CountDownLatch;

/**
 * [12.2] 예제2 — ThreadLocal로 같은 싱글톤을 동시 사용해도 로그가 안 섞인다(해결).
 *
 * 가설: Example1과 '완전히 같은 시나리오'(Tracer 하나를 3스레드가 동시 사용 + begin 후 sleep으로 경쟁
 * 강제)인데, Tracer만 ThreadLocalTracer로 바꾸면 각 스레드가 자기 칸의 traceId만 보므로 섞이지 않는다.
 *
 * Example1과의 차이: 오직 Tracer 구현뿐이다(필드 -> ThreadLocal). 시나리오·sleep·스레드 수는 동일하게
 * 두어, "상태 저장 위치를 바꾼 것만으로 동시성 문제가 사라진다"는 점을 대조적으로 보여준다.
 * synchronized 같은 잠금을 전혀 쓰지 않았는데도 안전하다 — 공유 자체를 안 하기 때문이다.
 */
public class Example2_ThreadLocalIsolation {

    public static void main(String[] args) throws InterruptedException {
        // Example1과 동일하게 Tracer는 단 하나(싱글톤). 단 구현만 ThreadLocal 기반.
        ThreadLocalTracer tracer = new ThreadLocalTracer();

        int threads = 3;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);

        System.out.println("== [해결] ThreadLocal Tracer를 3개 스레드가 동시 사용 ==");
        for (int i = 1; i <= threads; i++) {
            String requestName = "요청" + i;
            new Thread(() -> {
                try {
                    ready.countDown();
                    go.await();
                    tracer.begin(requestName);
                    Thread.sleep(20);     // Example1과 똑같이 경쟁을 강제하지만...
                    tracer.log("처리 중"); // 내 스레드의 칸만 읽으므로 내 traceId가 그대로 찍힌다
                } catch (InterruptedException ignored) {
                } finally {
                    tracer.clear();        // 요청 끝 -> 내 칸 비우기(예제3에서 이게 왜 필수인지 본다)
                }
            }, requestName + "-스레드").start();
        }

        ready.await();
        go.countDown();
        Thread.sleep(500);

        System.out.println("\n=> begin과 log의 traceId가 스레드마다 일관되게 유지된다(섞임 없음).");
        System.out.println("   원인: traceId/depth가 '스레드별 독립 저장소(ThreadLocal)'라 서로 공유되지 않음.");
        System.out.println("   synchronized 없이도 안전하다 — 잠그는 게 아니라 '아예 공유를 안 하기' 때문.");
    }
}
