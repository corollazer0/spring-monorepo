# BatchFlow 실행 가이드 (spring-batch-onboarding)

> Spring Batch가 처음인 분을 위한 **완전 초보자용** 가이드입니다.
> 그대로 따라하면 5분 안에 첫 배치 Job이 돌아갑니다.
> 무엇을 배우는 과정인지는 [필수 트랙 커리큘럼](../docs/batch/curriculum/01-BatchFlow-Essential-Curriculum.md)을 보세요.
>
> 💡 **선수 과정**: [TestCraft](../spring-test-onboarding/README.md) 수료를 권장합니다 —
> 배치 학습은 테스트로 진행되므로 JUnit5/AssertJ 기본기가 전제입니다.

---

## 0. 준비물

[TestCraft 가이드의 준비물](../spring-test-onboarding/README.md#0-준비물)과 동일합니다
(JDK 8+, Git, IntelliJ 권장 — Gradle은 wrapper 포함).

```bash
git clone https://github.com/corollazer0/spring-monorepo.git
cd spring-monorepo
```

## 1. 30초 검증 — 첫 배치가 도는지 확인

```bash
.\gradlew :spring-batch-onboarding:test
```

`BUILD SUCCESSFUL`이면 성공! (skipped는 여러분이 풀 exercise — 정상입니다)

> 🖥️ **배치는 화면이 없습니다.** 웹앱(TestCraft)과 달리 배치의 "실행"은 테스트가 기본
> 수단이고, "결과 확인"은 로그와 메타데이터 테이블(BATCH_*)입니다.
> 이 관점 전환 자체가 첫 학습 포인트입니다.

특정 Job 하나만 돌려보기 (Hello Batch):

```bash
.\gradlew :spring-batch-onboarding:test --tests "com.batchflow.step02.*"
```

테스트 로그에서 `>>>>> Hello, Spring Batch!` 를 찾아보세요 — 여러분의 첫 배치입니다.

## 2. IntelliJ로 열기

1. **Open** → `spring-monorepo` **최상위 폴더** 선택 → Gradle 동기화 대기
2. 테스트 실행: 테스트 파일의 초록 ▶ 버튼
   - 첫 실행 추천: `src/test/java/com/batchflow/step01/example/BatchInfrastructureTest.java`
3. 한글 깨짐 시: Settings → Editor → File Encodings → 전부 UTF-8

## 3. 학습 시작하기

```
1) docs/batch/curriculum/01-BatchFlow-Essential-Curriculum.md  ← 전체 지도 (여기서 시작!)
2) docs/batch/education/FOR-BatchFlow-Step01.md                ← Step 1 문서
3) step01/example 실행하며 따라잡기
4) step01/exercise 의 @Disabled 지우고 TODO 풀기
5) step01/answer 와 비교 → 다음 Step으로
```

- **필수 트랙**: Step 1~13 (약 2주, 하루 1~2시간) — 인프라 → Job 구조 → Chunk →
  휴면회원 전환 실전 → 오류 제어 → 재시작 → 정산 캡스톤
- **심화**: Step 14~15 (멀티스레드/병렬, 파티셔닝)
- 기존 50-Step 전체 커리큘럼(00 문서)은 심화 참조 자료입니다

## 4. 자주 막히는 것 (FAQ)

| 증상 | 해결 |
|------|------|
| `JobInstanceAlreadyCompleteException` | 같은 파라미터로 성공한 Job 재실행은 거부됩니다(설계!) — Step 3 문서 참고. 테스트에서는 `removeJobExecutions()` 또는 unique 파라미터 |
| 카운트 검증이 단독은 통과, 전체는 실패 | `@BeforeEach`의 `removeJobExecutions()` 누락 — 배치판 롤백입니다 |
| 데이터가 변해 있다? | 배치는 진짜 커밋합니다 — 변경 테스트는 원상복구가 세트 (Step 10) |
| Batch 5.x 예제가 컴파일 안 됨 | 이 모듈은 4.3.x — `JobBuilderFactory` 방식만 (Step 2 문서) |
| skipped 테스트 다수 | 정상! 여러분이 풀 exercise입니다 |

## 5. 더 보기

| 문서 | 내용 |
|------|------|
| [필수 트랙 커리큘럼](../docs/batch/curriculum/01-BatchFlow-Essential-Curriculum.md) | 학습 철학, Step 1~15 지도, 50-Step 매핑 |
| [전체 50-Step (심화 참조)](../docs/batch/curriculum/00-BatchFlow-Curriculum.md) | Phase 4~6 (성능/운영/실전 프로젝트) |
| [모듈 규칙](./CLAUDE.md) | 코드/테스트 작성 규약 (AI 협업 포함) |
| [계획/태스크](../docs/batch/plan/plan.md) | 의사결정 기록과 진행 현황 |
