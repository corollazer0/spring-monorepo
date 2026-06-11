# [Web Step 8] Actuator — 운영의 눈, 그리고 어디까지 보여줄 것인가

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: 🆕 spring-boot-starter-actuator, 노출 제한(exposure.include), 커스텀 HealthIndicator, 404 봉인 테스트
> **코드 위치**: `spring-web-onboarding/src/{main/java/com/webflow/common/health, test/java/com/webflow/step08}/`

---

## 1. Before We Start — "지금 서버 살아있나요?"

기능은 다 만들었습니다. 운영이 시작되면 질문의 종류가 바뀝니다:

- 로드밸런서: "이 인스턴스로 트래픽 보내도 되나?" (헬스 체크)
- 모니터링: "메모리/스레드/요청 수 지표 좀" (메트릭)
- 당직자: "DB는 붙어있고? 업로드 디스크는 안 찼고?" (의존물 상태)

이걸 매번 로그를 뒤져 답할 순 없습니다. Spring Boot **Actuator**는 서버의 건강
상태를 HTTP로 들여다보는 표준 창구입니다 — 의존성 하나면 끝.

단, 창구를 여는 순간 정반대의 질문이 따라옵니다: **"어디까지 보여줄 것인가?"**
`/actuator/env`에는 DB 비밀번호가, `/actuator/beans`에는 내부 구조가 통째로
들어있습니다. 이번 Step의 절반은 "여는 법", 절반은 "닫는 법"입니다.

## 2. What We're Building

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics   # 이 셋만! (env/beans/shutdown은 닫힌 채로)
  endpoint:
    health:
      show-details: always
```

```
common/health/UploadDirHealthIndicator.java  ← 우리 서비스만의 의존물 체크
src/test/java/com/webflow/step08/
├── example/UploadDirHealthIndicatorTest.java ← UP/DOWN 양면 (순수 단위)
├── example/ActuatorEndpointTest.java         ← 열린 곳 200 + 닫힌 곳 404
├── exercise/ActuatorContractExerciseTest.java ← 상세/단건 지표/shutdown 404
└── answer/ActuatorContractAnswerTest.java
```

## 3. Core Concepts

### 3-1. 🆕 노출 제한 — 기본은 닫힘, 여는 건 명시적으로

Boot 2.x의 기본 웹 노출은 health(+info)뿐입니다. `include: health,info,metrics`는
"우리가 의식적으로 고른 목록"이고, 그 외는 **존재 자체가 404**입니다.

| 엔드포인트 | 내용 | 열면 생기는 일 |
|---|---|---|
| health | 살았나/죽었나 | LB·모니터링의 생명선 (열어야 함) |
| metrics | JVM·HTTP 지표 | 모니터링 수집 (보통 엶) |
| env | **모든 설정값** | DB 접속 정보 노출 |
| beans | 전체 빈 구조 | 내부 설계도 노출 |
| shutdown | **서버 종료** | HTTP 한 방에 서버 다운 |

`*`로 다 여는 예제 코드가 인터넷에 많습니다 — 따라하면 안 되는 대표 사례.

### 3-2. 🆕 커스텀 HealthIndicator — 우리만 아는 의존물은 우리가 알린다

DB는 Boot가 자동으로 봐줍니다(DataSourceHealthIndicator → components.db).
하지만 "업로드 디스크가 쓰기 가능한가"는 우리 서비스만의 사정 — 우리가 만듭니다:

```java
@Component   // 빈 이름 uploadDirHealthIndicator → components."uploadDir"
public class UploadDirHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // 어떤 상황에서도 예외 없이 — UP/DOWN "값"으로 답한다
        return Health.up().withDetail("path", ...).build();
    }
}
```

규약 하나가 중요합니다: **health()는 절대 예외를 던지지 않는다.** 헬스 체크가
터지면 모니터링 자체가 눈을 잃습니다. 그래서 테스트의 DOWN 케이스에서
"예외가 안 터지고 DOWN 값이 온다"가 첫 번째 검증입니다.

컴포넌트 하나라도 DOWN이면 전체 status도 DOWN — 로드밸런서가 이 인스턴스를
풀에서 뺍니다. **HealthIndicator 하나가 트래픽 라우팅을 좌우**하는 셈이니,
무엇을 DOWN의 기준으로 삼을지는 신중하게.

### 3-3. DOWN을 어떻게 "연출"하나 — 장애 시뮬레이션의 기술

UP 테스트는 쉽습니다. DOWN은? 디스크를 뽑을 순 없으니 **결정적이고 OS 무관한**
실패를 연출합니다 — "디렉터리를 만들 자리에 같은 이름의 파일을 놓는다":

```java
Files.createFile(tempDir.resolve("upload"));            // 자리를 파일로 점거
new UploadDirHealthIndicator(occupied.toString()).health();  // → DOWN
```

Step 4의 `withException(SocketTimeoutException)`과 같은 계보 — **장애 연출도
테스트 설계 기술**입니다.

### 3-4. Actuator 테스트는 @SpringBootTest — 그리고 404도 계약이다

Actuator 엔드포인트는 우리 Controller가 아니라 관리 자동구성이 만듭니다 —
@WebMvcTest 슬라이스엔 없습니다. `@SpringBootTest + @AutoConfigureMockMvc`로
전체를 띄워 검증합니다.

그리고 이 Step의 백미는 **닫힌 문 테스트**:

```java
mockMvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
```

지금은 당연히 404지만, 누군가 "디버깅하게 잠깐 `*`로 열게요"라고 커밋하는 순간
이 테스트가 빨갛게 켜집니다. **보안 설정의 회귀 방지** — "없어야 할 것이
없는지"를 확인하는 것도 테스트의 일입니다.

### 3-5. 모니터링 응답도 API 계약이다

Prometheus 같은 수집기는 `/actuator/metrics` 응답 구조를 긁어가도록 설정됩니다.
health 응답의 `components.uploadDir.details.path`를 대시보드가 표시한다면
그것도 계약입니다. 응답 모양이 바뀌면? **알람이 침묵합니다** — 사람이 아니라
기계가 소비하는 API일수록 계약 테스트가 중요합니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-web-onboarding:test --tests "com.webflow.step08.*"
```

1. yml의 exposure.include — "고른 셋"의 의미
2. `UploadDirHealthIndicatorTest` — UP / 자가복구 UP / DOWN(파일 점거) 3면
3. `ActuatorEndpointTest` — 열린 곳(health 컴포넌트, metrics)과 닫힌 곳(404 셋)
4. **일부러 깨뜨려보기**: yml의 include에 env를 추가하면 어떤 테스트가 깨질까?
   → 보안 완화가 코드 리뷰 없이 통과할 수 없다는 뜻

## 5. Testing — exercise 풀기

`step08/exercise/ActuatorContractExerciseTest.java`의 TODO 1~3을 채우세요.
TODO 3(shutdown 404)이 핵심입니다 — **서버를 끄는 문이 닫혀 있는지**를
적극적으로 봉인하는 것. 보안 테스트의 절반은 부재(不在)의 확인입니다.

## 6. Lessons Learned

### 사례: 구글에 검색되던 /actuator/env

- **증상**: 보안 점검에서 외부 인터넷에서 env 엔드포인트 접근 가능 발견 —
  DB 접속 정보, 내부 API 키 전부 노출 상태
- **원인**: 개발 중 `exposure.include: "*"`로 열고 그대로 배포
- **해결**: include를 명시 목록으로 + 닫힌 문 404 테스트 추가 + 키 전면 교체
- **교훈**: Actuator는 양날의 검 — 노출 설정은 코드와 같은 무게로 리뷰하고,
  테스트로 회귀를 막아라.

### 사례: 헬스 체크가 만든 전면 장애

- **증상**: 외부 추천 API가 느려지자 서비스 전체가 LB에서 제외됨
- **원인**: 커스텀 HealthIndicator가 부가 기능(추천 API)까지 DOWN 기준에 포함 —
  부가 기능 장애가 "인스턴스 죽음"으로 번역됨
- **해결**: 핵심 의존물(DB, 디스크)만 DOWN 기준으로, 부가 정보는 detail로만
- **교훈**: HealthIndicator의 DOWN은 "트래픽 빼라"는 신호다. 그 무게에 맞는
  의존물만 기준으로 삼아라.

### 시니어의 시선

> 운영 준비 리뷰에서 Actuator 설정을 보면 팀의 성숙도가 보입니다.
> `*`는 논외고, 명시 목록이면 기본은 된 것. 닫힌 엔드포인트의 404 테스트까지
> 있으면 — "이 팀은 보안 설정도 회귀 관리를 하는구나" 하고 안심합니다.

## 7. Key Takeaways

- 노출은 명시적 include 목록으로 — env/beans/shutdown은 닫힌 채로 (`*` 금지)
- 커스텀 HealthIndicator: 우리만의 의존물(업로드 디스크)은 우리가 알린다
- health()는 예외를 던지지 않는다 — 어떤 상황에도 UP/DOWN 값으로
- DOWN은 트래픽 제외 신호 — 핵심 의존물만 기준으로
- 닫힌 문(404)도 테스트로 봉인 — 보안 설정의 회귀 방지
- 모니터링 응답도 API 계약 — 기계가 소비하는 구조일수록 계약 테스트

## 8. Next Steps — 다음 Step의 문제

필수 기술이 모두 모였습니다: CRUD, 페이징/검색, 외부 연동과 장애 생존, 파일,
캐싱, 스케줄링, 운영. 이제 **요구사항만 받고 스스로 설계·구현**할 차례입니다.

> "결제 완료된 주문의 **배송 조회** 기능을 붙여주세요. 배송사 API는 따로 있고요,
> 미결제 주문은 당연히 조회되면 안 됩니다."

어느 Step의 무기를 어디에 쓸지 스스로 결정하세요 —
**Step 9: 캡스톤, 배송 조회 연동**입니다.
