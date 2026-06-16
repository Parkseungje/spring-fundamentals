package com.study.part12_aop.s05_proxy_vs_decorator;

/**
 * [12.5] 데코레이터 2 — '기능 추가': 읽은 결과를 [대괄호]로 감싼다.
 *
 * UpperCaseDecorator와 같은 모양(target 보유 + 위임 + 결과 가공)이다. 둘을 '쌓으면' 기능이 누적된다:
 * new BracketDecorator(new UpperCaseDecorator(plain)) -> 대문자화 후 괄호로 감쌈.
 * 이렇게 작은 기능들을 조합해 쌓을 수 있다는 점이 데코레이터의 대표적 장점이다(자바 I/O가 그 사례 - Example3).
 */
public class BracketDecorator implements TextSource {

    private final TextSource target;

    public BracketDecorator(TextSource target) {
        this.target = target;
    }

    @Override
    public String read() {
        String result = target.read();
        String decorated = "[" + result + "]";
        System.out.println("    [데코:괄호] \"" + result + "\" -> \"" + decorated + "\"");
        return decorated;
    }
}
