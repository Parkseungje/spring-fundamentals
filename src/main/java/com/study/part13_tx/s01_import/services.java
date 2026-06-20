package com.study.part13_tx.s01_import;

/**
 * [13.1] @Import 데모용 단순 서비스 3종(한 파일에 모아 둠 — 데모 보조용).
 *
 * 13.1은 트랜잭션 심화(PART 13)에 들어가기 전, 스프링 설정을 '결합'하는 도구인 @Import를 짚는 단원이다.
 * 아래 서비스들은 서로 다른 @Configuration이 제공하는 빈으로 등록되어, @Import가 여러 설정을 어떻게
 * 한곳으로 모으는지 보여주는 데 쓰인다.
 */
class AppleService {
    String hello() {
        return "Apple";
    }
}

class BananaService {
    String hello() {
        return "Banana";
    }
}

class GreetingService {
    String hello() {
        return "Greeting(@EnableXxx via @Import)";
    }
}
