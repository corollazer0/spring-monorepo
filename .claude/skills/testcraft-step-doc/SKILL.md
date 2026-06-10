---
name: testcraft-step-doc
description: spring-test-onboarding(TestCraft) 모듈의 Step 학습 자료를 만들거나 수정할 때 사용 — FOR-Test-StepNN 문서 형식, example/exercise/answer 3종 테스트 패키지 규약, 커리큘럼 갱신 절차. 범용 교육 문서는 education-doc 스킬 사용.
---

# TestCraft Step 제작 표준 (모듈 특화)

> 베이스는 `education-doc` 스킬의 템플릿 — 이 스킬은 TestCraft 고유 규약을 추가한다.

## 1. Step 1개의 구성물 (한 커밋에 함께)

| 산출물 | 위치/규약 |
|--------|----------|
| 프로덕션 코드 | 해당 Step에서 "새로 등장"하는 코드만 — 미리 몰아서 만들지 않는다 (커밋=학습단위 1:1) |
| example 테스트 | `stepNN/example/{대상}Test` — 완성 모범, 풍부한 한국어 Javadoc(왜/검증내용), 항상 통과 |
| exercise 테스트 | `stepNN/exercise/{대상}ExerciseTest` — 클래스 레벨 `@Disabled("과제: ...문서 참고")` + given/when/then 골격 + 한국어 TODO 지시문. 컴파일은 항상 성공 |
| answer 테스트 | `stepNN/answer/{대상}AnswerTest` — exercise와 메서드명 1:1, "(TODO n 답)" 주석, 채점 포인트 Javadoc, 항상 통과 |
| 교육 문서 | `docs/test/education/FOR-Test-StepNN.md` — education-doc 템플릿 + 헤더에 권장 학습 순서 |

## 2. 문제 주도 연결 규칙

- 문서 1장(Before We Start)은 반드시 **앞 Step의 한계**에서 출발
- 마지막 장(Next Steps)은 다음 Step의 **문제를 예고** (cliffhanger)
- 구축 중 실제로 겪은 버그는 최우선 Lessons Learned 소재 — 그대로 기록한다

## 3. Step 추가/수정 시 갱신 체크리스트

- [ ] `docs/test/curriculum/00-TestCraft-Curriculum.md` — Step 표 행, 소요시간, (번호 순서와 학습 순서가 다르면) 권장 순서 명시
- [ ] `docs/test/skills/spring-test-annotations.md` — 새 도구/함정이 있으면 치트시트와 진단표에 추가
- [ ] 모듈 `CLAUDE.md` — 새 규약이 생겼으면 반영
- [ ] 기존 Step 번호 **리넘버링 금지** — 커밋/문서/패키지명이 참조 중. 새 Step은 끝 번호로 추가하고 권장 순서를 문서로 안내
- [ ] `.\gradlew :spring-test-onboarding:test` 전체 그린 (exercise는 skipped) + 두 번 연속 실행 확인

## 4. 학습 분량 기준

Step당 1~1.5시간(문서 20~30분 + example 20~30분 + exercise 20~30분 + answer 비교).
넘치면 Step을 쪼갠다 — 하루 1 Step 완결이 원칙(업무 병행 학습자 기준).
