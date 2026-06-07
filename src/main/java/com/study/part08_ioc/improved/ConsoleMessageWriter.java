package com.study.part08_ioc.improved;

import org.springframework.stereotype.Component;

@Component
public class ConsoleMessageWriter implements MessageWriter {

    @Override
    public void write(String message) {
        System.out.println("[console] " + message);
    }
}
