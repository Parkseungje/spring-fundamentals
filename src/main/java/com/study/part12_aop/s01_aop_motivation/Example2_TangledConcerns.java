package com.study.part12_aop.s01_aop_motivation;

/**
 * [12.1] 예제2 — 여러 부가 관심사(로깅+보안+트랜잭션)가 한 메서드에 '엉킨' 모습.
 *
 * Main(예제1)이 '로깅 하나의 흩어짐'을 보여줬다면, 여기서는 한 메서드에 부가 관심사가 여러 개 겹칠 때
 * 비즈니스 로직이 얼마나 파묻히는지(엉킴, tangling)를 보여준다. placeOrder의 '진짜' 일은 재고 차감 +
 * 주문 생성 두 줄뿐인데, 보안·트랜잭션·로깅 코드가 그 위아래를 둘러싼다.
 *
 * 가설: 정상(admin)은 보안 통과 -> 트랜잭션 begin -> 비즈니스 2줄 -> commit -> 로그 종료. 권한 없는
 * 사용자(guest)는 보안에서 막혀 비즈니스에 도달도 못 한다(부가 관심사가 흐름을 지배).
 */
public class Example2_TangledConcerns {

    public static void main(String[] args) {
        TangledOrderService service = new TangledOrderService(new LogTracer());

        System.out.println("== 정상(admin) — 보안·트랜잭션·로깅이 비즈니스 2줄을 둘러쌈 ==");
        service.placeOrder("admin", "노트북");

        System.out.println("\n== 권한 없음(guest) — 보안 관심사에서 막혀 비즈니스 도달 못 함 ==");
        try {
            service.placeOrder("guest", "노트북");
        } catch (SecurityException e) {
            System.out.println("    거부됨: " + e.getMessage());
        }

        System.out.println("\n=> 비즈니스는 '재고 차감 + 주문 생성' 두 줄뿐인데, 로깅+보안+트랜잭션이 뒤엉켜(tangling)");
        System.out.println("   메서드를 가득 채운다. 이 엉킨 덩어리가 모든 메서드에 또 반복(scattering)된다.");
        System.out.println("   관심사가 늘수록 고통이 곱으로 커진다 -> AOP로 부가 관심사를 떼어내야 하는 강한 동기.");
    }
}
