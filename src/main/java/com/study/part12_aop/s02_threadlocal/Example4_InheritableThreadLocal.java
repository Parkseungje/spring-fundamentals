package com.study.part12_aop.s02_threadlocal;

/**
 * [12.2] 예제4 — ThreadLocal 값은 '자식 스레드'로 전파되지 않는다 + InheritableThreadLocal로 전파.
 *
 * ThreadLocal은 '그 스레드의 칸'에만 값을 둔다(예제1~3). 그래서 부모 스레드에서 set한 값은, 부모가 만든
 * '자식 스레드'에서는 보이지 않는다(자식은 자기 칸이 비어 초기값을 봄). 비동기/병렬 처리에서 "부모에서
 * 넣은 traceId가 자식 작업에서 갑자기 사라지는" 흔한 함정이다.
 *
 * 해결책 중 하나가 InheritableThreadLocal이다: 자식 스레드를 '생성하는 시점'에 부모의 값을 자식 칸으로
 * 복사해 준다. 그래서 자식이 부모의 값을 이어받는다.
 *  - 주의: 어디까지나 '자식 생성 시점 복사'다. 스레드 풀처럼 '이미 만들어진 스레드를 재사용'하면 생성 시점이
 *    아니라 복사가 안 일어나, 풀 환경에선 InheritableThreadLocal도 한계가 있다(별도 전파 도구 필요).
 *
 * 가설: 일반 ThreadLocal은 자식 스레드에서 초기값('(none)')이 나오고, InheritableThreadLocal은 부모 값을 본다.
 */
public class Example4_InheritableThreadLocal {

    // 일반 ThreadLocal: 자식에게 전파 안 됨
    static final ThreadLocal<String> plain = ThreadLocal.withInitial(() -> "(none)");
    // InheritableThreadLocal: 자식 생성 시 부모 값 복사
    static final ThreadLocal<String> inheritable = new InheritableThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
        // 부모(main) 스레드에서 값 설정
        plain.set("부모-traceId");
        inheritable.set("부모-traceId");
        System.out.println("[main(부모)] plain=" + plain.get() + ", inheritable=" + inheritable.get());

        // 부모가 자식 스레드를 만든다. 자식에서 두 값을 읽어 본다.
        Thread child = new Thread(() -> {
            System.out.println("[child(자식)] plain       = " + plain.get()
                    + "   <- 일반 ThreadLocal: 부모 값 안 보임(자식 칸은 비어 초기값)");
            System.out.println("[child(자식)] inheritable = " + inheritable.get()
                    + "   <- InheritableThreadLocal: 부모 값을 이어받음");
        }, "child");
        child.start();
        child.join();

        System.out.println("\n=> ThreadLocal 값은 자식 스레드로 전파되지 않는다(자식은 자기 칸만 본다).");
        System.out.println("   InheritableThreadLocal은 '자식 생성 시점'에 부모 값을 복사해 전파한다.");
        System.out.println("   (단 스레드 풀 재사용에는 생성 시점이 없어 한계 -> 비동기 전파는 별도 도구 필요.)");
    }
}
