package com.study.part12_aop.s05_proxy_vs_decorator;

/**
 * [12.5] 예제1 — 데코레이터 패턴: '기능 추가'를 여러 겹 쌓는다.
 *
 * 가설: 원본(PlainTextSource)을 데코레이터로 감싸면 결과에 기능이 더해지고, 데코레이터를 '쌓으면' 기능이
 * 누적된다(대문자화 -> 괄호). 데코레이터는 항상 위임하고 결과만 가공한다(접근을 막지 않는다).
 *
 * 예제2(프록시)와의 대비 포인트: 구조(같은 인터페이스 구현 + target 위임)는 동일하지만, 여기서는 결과를
 * '꾸미는' 것이 의도다. 또 여러 개를 쌓아 조합하는 게 자연스럽다(프록시는 보통 접근 통제라 쌓기보다 단일).
 */
public class Example1_DecoratorPattern {

    public static void main(String[] args) {
        System.out.println("== [데코레이터] 기능 추가를 여러 겹 쌓기 ==");

        TextSource plain = new PlainTextSource("hello");

        // 데코레이터 쌓기: 대문자화한 뒤 괄호로 감싼다. (안쪽부터 바깥으로 적용)
        TextSource decorated = new BracketDecorator(new UpperCaseDecorator(plain));

        System.out.println("  최종 결과 = " + decorated.read());

        System.out.println("\n=> read() 호출이 원본 -> 대문자 -> 괄호 순으로 흐르며 기능이 누적됐다(hello -> HELLO -> [HELLO]).");
        System.out.println("   데코레이터는 '항상 위임 + 결과 가공', 그리고 '쌓아서 조합'하는 게 특징(기능 추가).");
    }
}
