---
name: step-commit
description: 이 저장소에서 커밋을 만들 때 사용 — 커밋 메시지 형식([모듈/Step N] + 학습 포인트), 커밋 단위 원칙(매 커밋 그린), plan.md/task.md 갱신 절차를 정의한다.
---

# 커밋 & 작업 단위 표준

## 1. 커밋 메시지 형식

```
[이모지] [type]: [모듈/Step N] 간단한 설명

상세 설명 (선택)

학습 포인트:
- 포인트 1
- 포인트 2
```

| type | 이모지 | type | 이모지 |
|------|--------|------|--------|
| feat | ✨ | refactor | ♻️ |
| fix | 🐛 | docs | 📝 |
| test | ✅ | config | ⚙️ |

- 학습/온보딩 모듈의 커밋에는 `학습 포인트` 섹션 필수 (커밋 히스토리 = 학습 경로)
- ⚠️ **커밋 메시지에 큰따옴표(") 금지** — Windows PowerShell 5.1이 네이티브 인자의 따옴표를 깨뜨린다. 한국어 따옴표나 작은따옴표로 대체

## 2. 커밋 단위 원칙

- 1 커밋 = 1 완결 단위 (학습 모듈은 1 Step = 1 커밋: 프로덕션 + 테스트 + 문서 동시)
- **모든 커밋은 그린**: 커밋 전 해당 모듈 `gradlew :모듈:test` 필수, 큰 변경은 루트 `gradlew test`
- 모듈 간 상호 참조를 만드는 커밋 금지 (루트 CLAUDE.md의 격리 규칙)

## 3. plan.md / task.md 갱신 절차 (필수 운영 규칙)

대상: `docs/{test,batch,web}/plan/{plan.md, task.md}` — 작업하는 모듈의 것을 갱신한다
(test=spring-test-onboarding / batch=spring-batch-onboarding / web=spring-web-onboarding)

1. **작업 시작 전**: plan.md에 계획 섹션 추가(배경/결정/작업 분해), task.md에 태스크를 `[ ]`로 등록
2. **작업 중**: 계획과 달라진 결정은 plan.md에 변경 사유와 함께 기록
3. **커밋 시**: 완료 태스크를 `[x]`로 체크하고 커밋 해시를 병기
4. plan/task 문서 갱신은 해당 작업 커밋에 포함하거나 직후 docs 커밋으로

## 4. 커밋 전 체크리스트

- [ ] 테스트 그린 (skipped는 @Disabled exercise만)
- [ ] 의도하지 않은 파일 포함 없음 (`git status` 확인)
- [ ] 메시지 형식/학습 포인트/따옴표 규칙 준수
- [ ] task.md 체크 갱신
