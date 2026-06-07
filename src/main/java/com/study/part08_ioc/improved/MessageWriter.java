package com.study.part08_ioc.improved;

/**
 * 8.4 인터페이스 + 합성 — Strategy.
 * MessagePrinter는 이 인터페이스에만 의존한다 (DIP).
 */
public interface MessageWriter {
    void write(String message);
}
