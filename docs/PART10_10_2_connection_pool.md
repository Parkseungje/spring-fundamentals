# PART 10 — DB 접근의 진화: 10.2 Connection Pool

> 이 문서는 커리큘럼 PART 10의 소단원 중 **10.2 Connection Pool**을 다룬다.
> 10.1에서 JDBC로 'DB에 어떻게 접근하나'를 표준화했다면, 10.2는 '연결을 매번 새로 만드는 비효율'을
> 연결 재사용(풀)으로 푼다. (실무 직결)

---

## 0. 들어가기 전에 — 핵심 용어
- **Connection(커넥션)**: 애플리케이션과 DB 사이의 연결 통로. 만들 때 TCP 연결 + 인증 + 세션 생성 비용이 든다.
- **Connection Pool(커넥션 풀)**: 연결을 미리 N개 만들어 두고 빌려주고(borrow)·반납(return)받아 '재사용'하는 저장소.
- **HikariCP**: Spring Boot의 기본 커넥션 풀 구현. (다른 구현: DBCP2, Tomcat JDBC Pool)
- **DB 세션(session)**: DB 입장에서 본 하나의 연결 단위. 풀의 Connection N개 = DB 세션 N개.
- **DB Lock(락)**: 트랜잭션 진행 중 다른 세션의 수정을 막는 잠금. 공유 락(S, 읽기) / 배타 락(X, 쓰기). DB에서도 데드락이 생긴다.

한 줄 그림: **연결을 매 요청마다 새로 만들면 '연결 비용'이 쿼리보다 더 들 수 있다. 미리 만들어 둔 연결을
빌려 쓰고 반납하는 '풀'을 쓰면 재사용으로 빠르다. Spring Boot는 HikariCP를 기본으로 쓴다.**

---

## 1. 학습 내용 — 매번 연결 vs 재사용

### 로우레벨의 비효율 — 연결은 비싸다
DB 연결 1회는 단순히 "선 하나 잇기"가 아니다. **TCP 3-way handshake + 인증(계정 확인) + DB 세션 생성**이
일어난다. 실제 원격 DB(MySQL/Oracle)에선 수~수백 ms가 걸리기도 한다. 그래서 매 요청마다 새 연결을 만들면
**연결 설정 시간이 정작 쿼리 실행 시간보다 더 길어지는** 어이없는 상황이 생긴다.

### 해결 — Connection Pool ('커넥션의 수영장')
- 애플리케이션 시작 시 연결을 **N개 미리 생성**해 풀에 담아 둔다.
- 요청이 오면 풀에서 하나 **빌리고(borrow)**, 다 쓰면 닫지 않고 풀에 **반납(return)**한다.
- 다음 요청은 그 연결을 **재사용** → 매번 생성 비용을 안 치른다. 동시 연결 수도 풀 크기로 제한돼 안정적.
- 대표 구현: **HikariCP(Spring Boot 기본)**, DBCP2, Tomcat JDBC Pool.
```java
HikariConfig cfg = new HikariConfig();
cfg.setJdbcUrl(url); cfg.setUsername("sa"); cfg.setMaximumPoolSize(10);
HikariDataSource pool = new HikariDataSource(cfg);
try (Connection c = pool.getConnection()) { ... }   // 빌림 -> try 종료 시 '반납'(close=반환)
```
> ★ 포인트: 풀에서 얻은 Connection의 `close()`는 '연결을 끊는' 게 아니라 '풀에 반납하는' 것이다. 그래서
> close해도 다음에 또 빌려 쓸 수 있다(PART 8.6 try-with-resources와 같은 모양이지만 의미는 '반납').

### 풀과 DB 세션·락 (개념)
- **풀의 Connection N개 = DB 세션 N개.** 한 Connection(세션) 안에서 **동시에 두 트랜잭션은 불가**하다
  (트랜잭션은 연결 단위 — 10.4와 연결).
- **DB Lock**: 한 트랜잭션이 행을 수정 중이면 다른 세션의 수정을 막는다(배타 락 X). 읽기는 공유 락(S).
  자바의 synchronized/락과 닮은꼴이고, **DB에서도 데드락이 생긴다**(PART 7.6의 데드락과 같은 원리, 무대가 DB일 뿐).

---

## 2. 실습으로 확인하기

> - **가설**: 같은 작업을 '매번 새 연결' vs '풀에서 재사용'으로 반복하면, 풀 쪽이 (연결 생성 비용을 안 치러) 더 빠르다.

### 코드 (`com.study.part10_db.s02_connection_pool`)
- `ConnectionPoolDemo` — 연결 얻기+가벼운 쿼리 2000회를 (A) DriverManager 매번 / (B) HikariCP 풀 재사용으로 비교.

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew runStage -Pmain=com.study.part10_db.s02_connection_pool.ConnectionPoolDemo
```

### 실행 결과 — 가설과 실제 비교 (실측)
```
[10.2] 연결 얻기+가벼운 쿼리 2000회 반복
  (A) 매번 새 연결(DriverManager) : 336 ms
  (B) 풀에서 재사용(HikariCP)     : 39 ms   (풀 크기 10, 연결 재사용)
```
- **인메모리 H2인데도 약 8.6배** 빠르다(336 → 39ms). 풀이 연결 생성 비용을 매번 안 치르기 때문. ✅
- 실제 원격 DB일수록 연결 비용(핸드셰이크·인증)이 커서 풀의 이득은 더 폭발적이다. (수치는 환경마다 다름)

---

## 3. 자기 점검

- **Q. 커넥션 풀이 빠른 이유는?**
  - 내 답: 연결(TCP 핸드셰이크+인증+세션 생성)을 미리 만들어 두고 재사용하므로, 매 요청마다 치르던 연결
    생성 비용을 없애기 때문. (실측 336ms→39ms)

- **Q. 풀에서 얻은 Connection을 close()하면 연결이 끊기나?**
  - 내 답: 아니다. '풀에 반납'된다. 그래서 다음 요청이 그 연결을 또 빌려 쓴다(재사용).

- **Q. 풀 크기와 DB 세션의 관계는?**
  - 내 답: 풀의 Connection N개 = DB 세션 N개. 한 연결(세션) 안에서 동시에 두 트랜잭션은 못 돌린다.
    그래서 동시 처리량과 DB 부하를 풀 크기로 조절한다.

- **Q. DB Lock/데드락은 PART 7과 무슨 관계인가?**
  - 내 답: 개념이 같다. 한 트랜잭션이 행을 잠그면 다른 세션이 기다리고, 서로 엇갈려 잠그면 데드락이 난다
    (7.6의 락·데드락과 동일 원리, 무대가 JVM이 아니라 DB일 뿐).
