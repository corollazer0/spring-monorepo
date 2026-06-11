# WebFlow 실행 가이드 (spring-web-onboarding)

> 실무 API 서버의 잡기술(외부 연동/파일/캐싱/스케줄링/운영)이 처음인 분을 위한
> **완전 초보자용** 가이드입니다. 그대로 따라하면 5분 안에 미니 커머스 API가 돌아갑니다.
> 무엇을 배우는 과정인지는 [커리큘럼](../docs/web/curriculum/00-WebFlow-Curriculum.md)을 보세요.
>
> 💡 **선수 과정**: [TestCraft](../spring-test-onboarding/README.md) 수료 권장 —
> 모든 학습이 테스트로 진행됩니다 (Mockito/@WebMvcTest/@MybatisTest 전제).

---

## 0. 준비물

[TestCraft 가이드의 준비물](../spring-test-onboarding/README.md#0-준비물)과 동일합니다
(JDK 8+, Git, IntelliJ 권장 — Gradle은 wrapper 포함).

```bash
git clone https://github.com/corollazer0/spring-monorepo.git
cd spring-monorepo
```

## 1. 30초 검증 — 전체 그린 확인

```bash
.\gradlew :spring-web-onboarding:test
```

`BUILD SUCCESSFUL`이면 성공! (skipped는 여러분이 풀 exercise — 정상입니다)

## 2. 직접 호출해보기 (bootRun)

```bash
.\gradlew :spring-web-onboarding:bootRun
```

서버가 뜨면 (포트 8080):

```bash
# 상품 목록 — 페이징·검색·정렬 (Step 2)
curl "http://localhost:8080/api/products?keyword=키보드&sort=priceAsc"

# 상품 단건 (Step 1, 두 번 부르면 캐시가 받는다 — Step 6)
curl http://localhost:8080/api/products/1

# 주문 생성 (Step 1)
curl -X POST http://localhost:8080/api/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 2}"

# 서버 건강 상태 (Step 8)
curl http://localhost:8080/actuator/health
```

> ⚠️ 결제(`POST /api/orders/{id}/payment`)와 배송 조회는 **가상 외부 API**
> (pg.example.com)를 부르므로 bootRun에선 실패(503)합니다 — 정상입니다!
> 외부 연동의 검증은 테스트(MockRestServiceServer)가 담당한다는 것 자체가
> Step 3의 첫 학습 포인트입니다.

- H2 콘솔: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:webdb;MODE=MSSQLServer`, 사용자 `sa`)

## 3. IntelliJ로 열기

1. **Open** → `spring-monorepo` **최상위 폴더** 선택 → Gradle 동기화 대기
2. 테스트 실행: 테스트 파일의 초록 ▶ 버튼
   - 첫 실행 추천: `src/test/java/com/webflow/step01/example/OrderServiceTest.java`
3. 한글 깨짐 시: Settings → Editor → File Encodings → 전부 UTF-8

## 4. 학습 시작하기

```
1) docs/web/curriculum/00-WebFlow-Curriculum.md   ← 전체 지도 (여기서 시작!)
2) docs/web/education/FOR-WebFlow-Step01.md       ← Step 1 문서
3) step01/example 실행하며 따라잡기
4) step01/exercise 의 @Disabled 지우고 TODO 풀기
5) step01/answer 와 비교 → 다음 Step으로
```

- **필수 Step 1~9** (약 2주, 하루 1~2시간):
  도메인 → 목록 API → 외부 결제 → 장애 생존 → 파일 → 캐싱 → 스케줄링 → Actuator → 캡스톤
- **Step 9 캡스톤**은 순서가 다릅니다 — answer를 보기 전에 요구사항만으로
  스스로 설계하세요 (FOR-WebFlow-Step09.md의 진행 방법 참고)

## 5. 자주 막히는 것 (FAQ)

| 증상 | 해결 |
|------|------|
| 결제/배송 API가 bootRun에서 503 | 정상! 가상 외부 주소(example.com)입니다 — 외부 연동 검증은 테스트로 (2장 참고) |
| 캐시 테스트가 단독은 통과, 전체는 실패 | `@BeforeEach` 캐시 clear 누락 — 캐시는 테스트 사이에 살아남습니다 (Step 6 문서) |
| `MockRestServiceServer` URL 불일치 | rootUri 사용 시 기대 URL은 **상대 경로**(`/api/v1/...`)로 (Step 3 문서) |
| 업로드 테스트 후 파일이 남는다? | example처럼 @TempDir을 쓰세요 — 실제 경로에 쓰면 청소는 여러분 몫 |
| `/actuator/env`가 404 | 의도된 동작! 노출 제한(include 목록)이 보안 설계입니다 (Step 8 문서) |
| skipped 테스트 다수 | 정상! 여러분이 풀 exercise입니다 |

## 6. 더 보기

| 문서 | 내용 |
|------|------|
| [커리큘럼](../docs/web/curriculum/00-WebFlow-Curriculum.md) | 학습 철학, Step 1~9 지도, 시그니처 테스트 기법 |
| [모듈 규칙](./CLAUDE.md) | 코드/테스트 작성 규약 (AI 협업 포함) |
| [계획/태스크](../docs/web/plan/plan.md) | 의사결정 기록과 진행 현황 |
| [테스트 치트시트](../docs/test/skills/spring-test-annotations.md) | 전 모듈 공통 어노테이션 정리 |
