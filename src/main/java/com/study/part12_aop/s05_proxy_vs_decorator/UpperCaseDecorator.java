package com.study.part12_aop.s05_proxy_vs_decorator;

/**
 * [12.5] 데코레이터 1 — '기능 추가': 읽은 결과를 대문자로 바꾼다.
 *
 * 데코레이터의 특징: 대상(target)에 '항상 위임'한 뒤, 그 결과에 기능을 더한다(여기선 toUpperCase).
 * 접근을 막지 않는다(그건 프록시). 또 데코레이터끼리 '쌓을' 수 있다 — target 자리에 또 다른 데코레이터를
 * 넣으면 기능이 누적된다(Example1에서 BracketDecorator와 함께 쌓는다).
 */
public class UpperCaseDecorator implements TextSource {

    private final TextSource target; // 진짜 객체이거나 또 다른 데코레이터(쌓기 가능)

    public UpperCaseDecorator(TextSource target) {
        this.target = target;
    }

    @Override
    public String read() {
        String result = target.read();          // 항상 위임(접근 막지 않음)
        String decorated = result.toUpperCase(); // 결과에 기능 추가
        System.out.println("    [데코:대문자] \"" + result + "\" -> \"" + decorated + "\"");
        return decorated;
    }
}
