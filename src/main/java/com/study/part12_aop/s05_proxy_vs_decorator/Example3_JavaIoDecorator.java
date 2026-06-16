package com.study.part12_aop.s05_proxy_vs_decorator;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * [12.5] 예제3 — 자바 I/O는 데코레이터 패턴의 대표 사례(PART 6과 연결).
 *
 * Example1의 '직접 만든 데코레이터'가 학습용 장난감이 아니라 JDK 표준 라이브러리에 실제로 쓰이는
 * 설계임을 확인한다. 자바 I/O(PART 6)의 스트림 래핑이 바로 데코레이터다:
 *  - InputStream(원본 바이트 스트림) <- 진짜 객체
 *  - InputStreamReader(바이트 -> 문자 변환 '기능 추가') <- 데코레이터
 *  - BufferedReader(버퍼링 + readLine() '기능 추가') <- 데코레이터
 * 각 래퍼는 안쪽 스트림에 위임하면서 기능을 더하고, '쌓아서' 조합한다(Example1과 똑같은 구조).
 *
 * 주의: 이들은 '접근 제어'가 아니라 '기능 추가'다(바이트->문자, 버퍼링, 줄 단위 읽기). 그래서 프록시가
 * 아니라 데코레이터로 분류한다 -- 모양이 아니라 '의도'로 구분하는 12.5의 기준 그대로다.
 */
public class Example3_JavaIoDecorator {

    public static void main(String[] args) throws IOException {
        System.out.println("== [자바 I/O] 데코레이터 쌓기: InputStream -> Reader -> BufferedReader ==");

        // 원본(진짜 객체): 바이트 스트림
        InputStream rawBytes = new ByteArrayInputStream(
                "first line\nsecond line".getBytes(StandardCharsets.UTF_8));

        // 데코레이터 1: 바이트 -> 문자 변환 기능 추가(InputStreamReader)
        InputStreamReader reader = new InputStreamReader(rawBytes, StandardCharsets.UTF_8);

        // 데코레이터 2: 버퍼링 + readLine() 기능 추가(BufferedReader). reader를 감싼다(쌓기).
        try (BufferedReader buffered = new BufferedReader(reader)) {
            String line;
            int n = 1;
            while ((line = buffered.readLine()) != null) { // readLine은 BufferedReader가 더한 기능
                System.out.println("  " + (n++) + "번째 줄: " + line);
            }
        }

        System.out.println("\n=> InputStream을 Reader로(바이트->문자), 다시 BufferedReader로(줄 단위) '쌓아' 감쌌다.");
        System.out.println("   Example1의 데코레이터와 똑같은 구조이며, '기능 추가'가 의도라 데코레이터로 분류된다.");
        System.out.println("   (PART 6 자바 I/O가 곧 데코레이터 패턴의 실전 사례.)");
    }
}
