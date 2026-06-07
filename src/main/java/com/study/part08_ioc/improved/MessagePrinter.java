package com.study.part08_ioc.improved;

import org.springframework.stereotype.Component;

/**
 * 8.4~8.6 — 생성자 주입(권장).
 * 어떤 MessageWriter 구현체를 쓸지는 이 클래스가 결정하지 않는다 (IoC).
 * Spring 컨테이너(ApplicationContext)가 구현체를 선택해 주입한다.
 */
@Component
public class MessagePrinter {

    private final MessageWriter writer;

    public MessagePrinter(MessageWriter writer) {
        this.writer = writer;
    }

    public void send(String message) {
        writer.write(message);
    }
}
