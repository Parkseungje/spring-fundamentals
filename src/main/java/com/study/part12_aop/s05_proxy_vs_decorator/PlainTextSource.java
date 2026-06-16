package com.study.part12_aop.s05_proxy_vs_decorator;

/**
 * [12.5] 진짜 객체(real subject) — 원본 텍스트를 그대로 돌려주는 순수 컴포넌트.
 * 데코레이터도 프록시도 이 객체를 감싸지만, 이 클래스 자체는 둘의 존재를 모른다(원본 불변).
 */
public class PlainTextSource implements TextSource {

    private final String text;

    public PlainTextSource(String text) {
        this.text = text;
    }

    @Override
    public String read() {
        System.out.println("    [원본] read() 호출 -> \"" + text + "\" 반환");
        return text;
    }
}
