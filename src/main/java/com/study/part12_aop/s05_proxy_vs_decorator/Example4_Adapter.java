package com.study.part12_aop.s05_proxy_vs_decorator;

/**
 * [12.5] 예제4 — 어댑터(Adapter) 패턴: 감싸는 건 같지만 '인터페이스를 변환'한다(프록시/데코레이터와의 차이).
 *
 * 프록시·데코레이터·어댑터는 모두 "객체를 감싸는(wrapper)" 형제다. 결정적 차이:
 *   - 프록시/데코레이터: 감싸도 '같은 인터페이스(TextSource)'를 유지한다 -> 클라이언트는 못 알아챈다.
 *   - 어댑터: '다른 인터페이스'를 'TextSource'로 '변환'한다 -> 호환되지 않던 둘을 연결한다(타입이 바뀐다).
 *
 * 여기 LegacyMessage는 TextSource와 무관한 옛 클래스로, read()가 아니라 fetch()라는 다른 메서드를 가진다.
 * 이걸 TextSource를 기대하는 코드에 끼우려면, fetch()를 read()로 '변환'해 주는 어댑터가 필요하다.
 *   - 즉 어댑터는 '기능 추가'(데코레이터)도 '접근 제어'(프록시)도 아니다. 단지 '맞지 않는 인터페이스를 맞춰주는' 것.
 *
 * (자바 예: InputStreamReader는 사실 '바이트 스트림 -> 문자 스트림'으로 변환하는 어댑터 성격도 있다.
 *  하지만 12.5에서는 '기능 추가' 의도로 보아 데코레이터로 분류했다 — 의도로 분류한다는 원칙 그대로.)
 */
public class Example4_Adapter {

    // TextSource와 '호환되지 않는' 옛 클래스 — 메서드 이름부터 다르다(read가 아니라 fetch).
    static class LegacyMessage {
        String fetch() {
            return "legacy-data";
        }
    }

    // 어댑터: LegacyMessage를 TextSource로 '변환'한다(fetch -> read).
    static class LegacyMessageAdapter implements TextSource {
        private final LegacyMessage adaptee;   // 변환 대상(다른 인터페이스)

        LegacyMessageAdapter(LegacyMessage adaptee) {
            this.adaptee = adaptee;
        }

        @Override
        public String read() {
            return adaptee.fetch();   // 다른 메서드(fetch)를 TextSource의 read로 이어 붙임 = 인터페이스 변환
        }
    }

    public static void main(String[] args) {
        System.out.println("== [어댑터] 다른 인터페이스(LegacyMessage.fetch)를 TextSource(read)로 변환 ==");

        LegacyMessage legacy = new LegacyMessage();   // TextSource가 아님(타입 호환 X)
        // legacy를 TextSource 자리에 바로 못 넣는다 -> 어댑터로 감싸 변환한다.
        TextSource adapted = new LegacyMessageAdapter(legacy);

        System.out.println("  adapted.read() = " + adapted.read() + "   <- fetch()를 read()로 변환해 호출됨");

        System.out.println("\n=> 어댑터는 '인터페이스를 변환'한다(타입이 LegacyMessage -> TextSource로 바뀜).");
        System.out.println("   프록시/데코레이터는 '같은 인터페이스를 유지'(감싸도 TextSource 그대로)하는 점이 다르다.");
        System.out.println("   감싸는 모양은 셋 다 비슷하지만 의도가 다르다: 기능추가(데코)/접근제어(프록시)/인터페이스변환(어댑터).");
    }
}
