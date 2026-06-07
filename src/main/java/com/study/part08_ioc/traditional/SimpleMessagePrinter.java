package com.study.part08_ioc.traditional;

/**
 * 로우레벨 버전 — 이 클래스가 "어떤 방식으로 출력할지"를 직접 결정(new)한다.
 * 콘솔 출력을 파일 출력으로 바꾸려면 이 클래스 자체를 수정해야 한다 (OCP 위반).
 */
public class SimpleMessagePrinter {

    private final ConsoleWriter writer = new ConsoleWriter();

    public void send(String message) {
        writer.write(message);
    }
}
