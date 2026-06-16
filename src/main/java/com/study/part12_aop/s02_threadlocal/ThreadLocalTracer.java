package com.study.part12_aop.s02_threadlocal;

/**
 * [12.2] ThreadLocal로 상태를 '스레드마다 따로' 저장하는 로그 추적기 — 싱글톤이어도 안전한 버전.
 *
 * FieldBasedTracer의 문제는 traceId/depth를 '인스턴스 필드'(모든 스레드가 공유)에 둔 것이다.
 * ThreadLocal은 같은 변수를 선언해도 '스레드마다 독립된 칸'을 준다. A 스레드가 set한 값은 A만 get하고,
 * B는 B의 값만 본다 → synchronized로 잠그지 않아도 서로 안 섞인다(공유 자체를 안 하니까).
 *
 * 비유: 학교 사물함. 변수 이름(ThreadLocal 객체)은 '사물함 줄' 하나지만, 각 학생(스레드)은 자기 칸만
 * 열고 닫는다. 같은 줄을 봐도 내용물은 학생마다 다르다.
 *
 * ★ 치명적 함정 — remove() 필수: 스레드 풀(PART 7.8)은 스레드를 '재사용'한다. 요청이 끝나도 ThreadLocal
 * 값을 지우지 않으면, 그 스레드가 '다음 요청'을 처리할 때 이전 요청의 값이 그대로 남아 있다 → 다른
 * 사용자의 데이터가 노출되는 보안 사고. 그래서 요청 끝에 반드시 finally에서 remove()로 칸을 비운다.
 * (이 함정의 실제 재현은 Example3에서 본다.)
 */
public class ThreadLocalTracer {

    // 같은 ThreadLocal 객체지만, get/set은 '호출한 스레드의 칸'에만 작용한다.
    // withInitial: 그 스레드가 처음 get할 때 채워질 초기값.
    private final ThreadLocal<String> traceId = ThreadLocal.withInitial(() -> "(none)");
    private final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);

    public void begin(String requestName) {
        // 내 스레드의 칸에만 새 값을 넣는다 → 다른 스레드의 칸은 건드리지 않는다.
        traceId.set(requestName + "-" + (int) (Math.random() * 1000));
        depth.set(0);
        log("begin");
    }

    public void log(String message) {
        // 내 스레드의 칸에서만 읽는다 → 다른 스레드가 바꿔도 영향 없음(FieldBasedTracer와의 결정적 차이).
        String indent = "  ".repeat(Math.max(depth.get(), 0));
        System.out.println("[" + Thread.currentThread().getName() + "] traceId=" + traceId.get()
                + " " + indent + "|" + message);
        depth.set(depth.get() + 1);
    }

    // 요청 처리가 끝나면 반드시 호출 — 내 스레드의 칸을 비운다(풀 재사용 시 누수 방지).
    public void clear() {
        traceId.remove();
        depth.remove();
    }
}
