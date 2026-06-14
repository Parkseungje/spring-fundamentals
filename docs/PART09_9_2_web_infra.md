# PART 9 — 테스트와 웹 인프라: 9.2 웹 인프라 기초

> 이 문서는 커리큘럼 PART 9의 소단원 중 **9.2 웹 인프라 기초**를 다룬다.
> Spring MVC(이후 PART)를 배우기 전에 알아야 할 웹 백엔드 인프라 '용어'를 정리한다. 코드보다 개념 위주이며,
> 마지막에 "Spring Boot JAR에 WAS가 내장된다"는 점만 실제로 확인한다.

---

## 0. 들어가기 전에 — 핵심 용어 한눈에
- **웹서버(Web Server)**: 정적 자원(HTML/CSS/이미지)을 그대로 돌려주는 서버. 예: Nginx, Apache.
- **WAS(Web Application Server)**: 동적 자원(코드 실행 결과)을 만들어 돌려주는 서버. 예: Tomcat, Jetty, Undertow.
- **서블릿(Servlet)**: 자바로 HTTP 요청을 처리하는 클래스(WAS가 실행).
- **JSP**: HTML 안에 자바를 넣는 기술. 실행 시 서블릿으로 변환된다.
- **SSR(Server-Side Rendering)**: 서버가 완성된 HTML을 만들어 보냄.
- **CSR(Client-Side Rendering)**: 서버는 데이터(JSON)만 주고, 브라우저의 JS가 화면을 그림.
- **JAR / WAR**: 자바 배포 패키지 두 종류(아래 표). Spring Boot는 JAR 권장(WAS 내장).

---

## 1. 학습 내용 — 웹 백엔드 인프라

### 1-1. 웹서버 vs WAS — 정적이냐 동적이냐
| | 웹서버 | WAS |
|---|---|---|
| 역할 | 정적 자원(HTML/CSS/이미지)을 그대로 전달 | 동적 자원(코드 실행 결과)을 생성해 전달 |
| 예시 | Nginx, Apache | Tomcat, Jetty, Undertow |

- **정적 vs 동적**: "이미 만들어진 파일을 그대로 줌"(정적) vs "요청마다 코드를 실행해 결과를 만들어 줌"(동적).
  예: `logo.png`는 정적(웹서버), "로그인한 사용자 이름이 박힌 페이지"는 동적(WAS).
- **실무 구성**: `[클라이언트] ↔ [Nginx(정적·리버스 프록시)] ↔ [Tomcat(동적)] ↔ [DB]`.
  웹서버가 앞단에서 정적 파일·부하 분산을 맡고, 동적 처리는 WAS로 넘긴다(역할 분담).

### 1-2. 서블릿 / JSP — 자바로 웹 요청 처리하기, 그 진화
- **서블릿(Servlet)**: 자바로 HTTP 요청을 받아 응답을 만드는 클래스. WAS(Tomcat)가 이 서블릿을 실행한다.
- **JSP**: HTML 안에 자바 코드를 섞어 쓰는 기술. **실행(컴파일) 시점에 서블릿으로 변환**되어 동작한다.
  (즉 JSP도 결국 서블릿이다.)
- **진화 과정**:
  1. 서블릿만 사용 → 자바 코드 안에서 HTML을 문자열로 찍어 가독성 최악.
  2. JSP만 사용 → HTML 안에 자바(비즈니스 로직)가 뒤섞여 유지보수 지옥.
  3. **MVC 패턴** → JSP=View(화면), 서블릿=Controller(로직)로 역할 분리. (PART 1의 SRP가 웹에서도 적용)
  4. 현대: JSP 대신 **Thymeleaf** 같은 템플릿 엔진, 또는 아예 CSR(React/Vue).

### 1-3. SSR vs CSR — 화면을 누가 그리나
| | SSR | CSR |
|---|---|---|
| 화면 생성 | 서버가 완성된 HTML을 만들어 보냄 | 서버는 데이터(JSON)만, 브라우저의 JS가 그림 |
| 초기 로딩 | 빠름(완성된 HTML이 바로 옴) | 느림(JS 받고 실행한 뒤 그림) |
| SEO(검색 노출) | 좋음(HTML이 이미 완성) | 까다로움(JS 실행 후에야 내용) |
| 예시 | JSP, Thymeleaf, Next.js | React/Vue SPA |

- **핵심 구분**: "HTML을 어디서 완성하느냐". 서버(SSR) vs 브라우저(CSR).
- 백엔드 관점: SSR이면 서버가 화면까지, CSR이면 서버는 **API로 데이터(JSON)만** 주고 화면은 프론트가 담당.
  (요즘 백엔드는 CSR용 REST API + JSON이 많다 — PART 6.6에서 본 "웹 통신은 JSON 직렬화"와 연결.)

### 1-4. JAR vs WAR — 배포 패키지
| | JAR | WAR |
|---|---|---|
| 포함 | 컴파일된 class + 라이브러리 | + JSP/Servlet/WEB-INF (웹 자원) |
| 실행 | `java -jar`로 단독 실행(JRE만 있으면) | 외부 WAS(Tomcat)에 올려야 실행 |

- **Spring Boot는 JAR 권장**: WAS(Tomcat)를 **JAR 안에 내장**한다. 그래서 외부에 Tomcat을 따로 설치·설정할
  필요 없이 `java -jar app.jar`만으로 서버가 뜬다 → 배포·실행이 단순(DevOps 부담 ↓). 마이크로서비스에서 특히 선호.
- 전통(WAR): 별도로 깔아둔 Tomcat에 war 파일을 배포. Spring Boot 이전의 일반적 방식.

> ★ 핵심 한 줄: **Spring Boot = "실행 가능한 JAR + 내장 WAS"**. `java -jar` 하나로 웹서버까지 같이 뜨는 게
> 전통 방식(외부 WAS에 WAR 배포)과의 결정적 차이다.

---

## 2. 실습으로 확인하기 — "JAR에 WAS가 내장된다"

> - **가설**: Spring Boot의 실행 JAR 안에는 Tomcat(WAS)이 라이브러리로 포함되어 있다(그래서 외부 WAS 불필요).

### 실행
**프로젝트 루트(`C:\develop\study\spring-fundamentals`)에서 실행**한다.
```bash
./gradlew bootJar
jar tf build/libs/spring-fundamentals-1.0-SNAPSHOT.jar
```
(`jar tf`는 JAR 내부 파일 목록을 보여준다. `tomcat-embed`로 필터하면 내장 톰캣이 보인다.)

### 실행 결과 — 가설과 실제 비교
```
build/libs/spring-fundamentals-1.0-SNAPSHOT.jar   (약 47MB — 라이브러리까지 다 들어 있어 큼)
BOOT-INF/lib/tomcat-embed-core-10.1.30.jar        ← 내장 Tomcat(WAS)!
BOOT-INF/lib/tomcat-embed-el-10.1.30.jar
BOOT-INF/lib/tomcat-embed-websocket-10.1.30.jar
```
- JAR 안에 `tomcat-embed-*`(내장 WAS)가 포함돼 있다. ✅ → 외부 Tomcat 설치 없이 `java -jar`만으로
  웹서버가 뜬다는 의미(이 프로젝트는 web 스타터가 있어 `java -jar` 시 8080에서 Tomcat이 기동된다).
- 이것이 "Spring Boot는 JAR 권장" + "WAS 내장"의 실체다.

---

## 3. 자기 점검

- **Q. 웹서버와 WAS의 차이는?**
  - 내 답: 웹서버는 정적 자원(HTML/이미지)을 그대로 전달(Nginx/Apache), WAS는 코드를 실행해 동적 결과를
    만들어 전달(Tomcat/Jetty). 실무는 앞단 웹서버 + 뒷단 WAS로 역할을 나눈다.

- **Q. JSP와 서블릿의 관계는?**
  - 내 답: JSP는 HTML 안에 자바를 넣는 기술이고, 실행 시 서블릿으로 변환된다. 즉 JSP도 결국 서블릿이다.
    진화: 서블릿만 → JSP만 → MVC(JSP=View, 서블릿=로직) → Thymeleaf/CSR.

- **Q. SSR과 CSR의 핵심 차이와 트레이드오프는?**
  - 내 답: HTML을 서버가 완성(SSR)하느냐 브라우저 JS가 그리느냐(CSR). SSR은 초기 로딩·SEO 유리, CSR은
    초기 로딩이 느리고 SEO가 까다롭지만 풍부한 상호작용에 유리. 백엔드는 CSR이면 JSON API만 제공.

- **Q. Spring Boot가 JAR를 권장하는 이유는?**
  - 내 답: WAS(Tomcat)를 JAR에 내장해서, 외부 WAS 설치·설정 없이 `java -jar`만으로 서버가 뜬다. 배포·실행이
    단순해 DevOps 부담이 줄고 마이크로서비스에 적합. (실습에서 JAR 안 tomcat-embed로 확인)
