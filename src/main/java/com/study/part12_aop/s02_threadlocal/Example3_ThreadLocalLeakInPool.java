package com.study.part12_aop.s02_threadlocal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * [12.2] 예제3 — ★ 치명적 함정: 스레드 풀 재사용 + remove() 누락 = 이전 사용자 데이터 노출.
 *
 * ThreadLocal은 '스레드별' 저장소다. 그런데 스레드 풀(PART 7.8)은 스레드를 죽이지 않고 '재사용'한다.
 * 그래서 요청이 끝나도 ThreadLocal 값을 지우지 않으면, 같은 스레드가 '다음 요청'을 처리할 때 이전
 * 요청이 남긴 값을 그대로 보게 된다. 실무에선 이게 "A 사용자의 인증 정보/주문이 B 사용자에게 보이는"
 * 심각한 보안 사고로 이어진다.
 *
 * 재현 방법: 스레드를 '1개'만 가진 풀을 만든다 -> 두 요청이 반드시 '같은 스레드'에서 순차 실행된다
 * (재사용 강제). 첫 요청이 traceId를 set하고, 한 버전은 clear()를 빼먹고 한 버전은 finally에서 clear()한다.
 * 두 번째 요청은 begin 없이 log만 호출해 "현재 trace 문맥"을 읽는다(요청 도중 어디선가 trace를 참조하는
 * 상황을 단순화). 그때 무엇이 보이는지로 누수 여부가 드러난다.
 */
public class Example3_ThreadLocalLeakInPool {

    public static void main(String[] args) throws InterruptedException {
        ThreadLocalTracer tracer = new ThreadLocalTracer();

        // (1) remove() 누락 — 누수 발생
        System.out.println("== [함정] 요청 끝에 remove() 안 함 + 풀이 스레드를 재사용 ==");
        ExecutorService pool = Executors.newFixedThreadPool(1); // 스레드 1개 -> 두 요청이 같은 스레드 재사용

        pool.submit(() -> {
            tracer.begin("사용자A요청");  // traceId를 'A'로 set
            tracer.log("A 처리");
            // clear() 호출을 '일부러 빼먹음' -> 스레드 칸에 A의 값이 그대로 남는다
        });
        pool.submit(() -> {
            // 두 번째 요청. begin을 호출하지 않고 현재 문맥을 읽기만 한다(어딘가에서 trace 참조).
            // 같은 스레드라 A가 남긴 traceId가 그대로 보인다 -> 다른 사용자 데이터 노출!
            tracer.log("B 처리(나는 begin도 안 했는데...)");
        });
        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);
        System.out.println("=> B의 로그에 'A'의 traceId가 찍힌다. A의 데이터가 B에게 샜다(보안 사고).\n");

        // (2) finally에서 remove() — 해결
        System.out.println("== [해결] 요청 끝 finally에서 clear()(remove) 호출 ==");
        ThreadLocalTracer safe = new ThreadLocalTracer();
        ExecutorService pool2 = Executors.newFixedThreadPool(1);

        pool2.submit(() -> {
            try {
                safe.begin("사용자A요청");
                safe.log("A 처리");
            } finally {
                safe.clear(); // 요청 끝 -> 내 칸 비움. 다음 요청이 재사용해도 깨끗하다.
            }
        });
        pool2.submit(() -> {
            // 이번엔 A가 칸을 비웠으므로, begin 없이 읽으면 초기값 "(none)"이 보인다(누수 없음).
            safe.log("B 처리(초기값이어야 정상)");
            safe.clear();
        });
        pool2.shutdown();
        pool2.awaitTermination(2, TimeUnit.SECONDS);
        System.out.println("=> B의 traceId가 '(none)'(초기값)이다. A의 흔적이 안 남았다. ★ remove()는 선택이 아니라 필수.");
    }
}
