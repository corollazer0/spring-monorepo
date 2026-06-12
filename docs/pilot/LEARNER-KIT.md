# TestCraft 파일럿 학습자 키트 (이 문서 하나로 시작하세요)

> 환영합니다! 여러분은 이 학습 과정의 **첫 번째 학습자이자 공동 제작자**입니다.
> 여러분이 막히는 모든 지점은 여러분의 부족함이 아니라 **커리큘럼의 개선 기회**입니다 —
> 그래서 "막힘의 기록"이 이 파일럿의 가장 귀한 산출물입니다.

---

## 1. 시작 순서 (Day 0 — 약 30분)

```
① 이 레포를 GitHub에서 "Fork" 한다 (본인 계정으로)   ← 중요! 아래 3장
② 포크를 클론: git clone https://github.com/{내계정}/spring-monorepo.git
③ 30초 검증:  .\gradlew :spring-test-onboarding:test   → BUILD SUCCESSFUL 확인
④ 웹앱 구경:  .\gradlew :spring-test-onboarding:bootRun → http://localhost:8080/posts
⑤ 진행 로그 준비: docs/pilot/PILOT-LOG-TEMPLATE.md 를 복사해
   docs/pilot/MY-PILOT-LOG.md 로 만들고 첫 커밋
```

상세 환경 안내(JDK/IntelliJ/FAQ): [spring-test-onboarding/README.md](../../spring-test-onboarding/README.md)
③에서 막히면 그것부터가 기록 대상입니다 — 로그에 적고 운영자에게 알려주세요.

## 2. 학습 경로 (약 2주, 하루 1~2시간)

```
docs/test/curriculum/00-TestCraft-Curriculum.md   ← 전체 지도 (가장 먼저!)
→ Step 1부터: 문서(FOR-Test-StepNN) 읽기 → example 실행·이해
  → exercise의 @Disabled 지우고 TODO 풀기 → answer와 비교 → 다음 Step
```

- **이번 파일럿 범위: 필수 코스 = Step 1~9 + 12** (심화 10~14는 완주 후 자유)
- Step 12(View)는 번호와 달리 필수입니다 — 권장 시점은 Step 5와 6 사이

## 3. 규칙 4가지 (이것만은 꼭)

1. **포크에서 작업하세요** — exercise를 푼 결과가 원본(main)에 합쳐지면
   다음 학습자의 문제가 사라집니다. 원본에는 PR을 보내지 마세요 (피드백은 로그로!)
2. **30분 룰** — exercise에 30분 이상 막히면 answer를 보세요. 단, 보고 이해한 뒤
   **answer를 닫고 다시 스스로 작성**합니다. (붙잡고 있는 시간은 미덕이 아닙니다)
3. **막힘은 즉시 기록** — 해결됐어도 기록하세요. "어디서, 무엇으로 해결했나"가
   이 파일럿의 핵심 데이터입니다 (MY-PILOT-LOG.md)
4. **AI(Claude 등) 사용 규칙** — "이 코드가 왜 이렇게 동작해?" 같은 **설명 요청은
   자유**, exercise의 **답 생성은 금지**입니다. AI가 푼 문제는 여러분 것이 되지 않습니다.

## 4. 진행 보고 (가볍게)

- **Step 완료마다**: 포크에 커밋 (형식 자유 — 레포의 커밋 규약을 따라 해보면 보너스)
  + MY-PILOT-LOG.md의 해당 Step 행 채우기 (1분 분량)
- **주 2회**: 운영자와 15분 체크인 (로그 보며 같이 훑기)
- 급한 막힘: 운영자에게 바로 — 단, 그 전에
  [진단표](../test/skills/spring-test-annotations.md)(문서 3장)를 먼저 한 번 보세요.
  진단표로 풀리면 그것도 로그에 "진단표로 해결"이라고 적어주세요!

## 5. 완주하면

- `.\gradlew :spring-test-onboarding:test` 그린 + 모든 exercise 통과가 졸업 조건
- 30분 종합 인터뷰 (로그의 마지막 장을 미리 채워오시면 빠릅니다)
- 원하면 심화(10~14) 또는 다음 모듈(BatchFlow)로 — 여러분 선택입니다

> 다시 한번 — **여러분을 평가하는 자리가 아닙니다. 커리큘럼을 평가하는 자리입니다.**
> 편하게 막히고, 솔직하게 기록해주세요. 그게 기여입니다.
