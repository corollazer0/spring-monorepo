# Education Guide Skill
## 온보딩 교육자료 작성 지침

---

## 🎯 이 스킬은 언제 사용하나요?

- 모든 Step 구현 후 교육 가이드 문서 작성 시
- 커밋과 함께 학습 자료를 생성할 때
- 팀원 온보딩용 문서가 필요할 때

---

## 📋 문서 작성 원칙

### 1. 파일명 규칙
```
docs/guides/FOR-BatchFlow-Step[번호].md
예: FOR-BatchFlow-Step09.md
```

### 2. 필수 포함 내용

| 섹션 | 설명 |
|------|------|
| **Before We Start** | 왜 이것을 배워야 하는지, 실무 연관성 |
| **What We're Building** | 이번 Step의 결과물 미리보기 |
| **Core Concepts** | 핵심 개념 설명 (비유 활용) |
| **Step-by-Step Implementation** | 구현 과정 상세 설명 |
| **Testing** | 테스트 코드와 실행 결과 |
| **Lessons Learned** | 버그, 함정, 해결 과정 |
| **Key Takeaways** | 3-5개 핵심 포인트 |

### 3. 작성 스타일

**DO ✅**
- 비유와 일화로 개념 설명
- 실제 발생한 버그와 해결 과정 포함
- 시니어 개발자의 사고방식 공유
- 대화체로 친근하게 작성

**DON'T ❌**
- 딱딱한 기술 문서 스타일
- 코드만 나열
- "~입니다", "~합니다"만 반복
- 이론만 설명하고 실습 없음

### 4. 예시: 좋은 설명 vs 나쁜 설명

❌ **나쁜 예**:
> "Chunk는 일정 단위로 데이터를 처리하는 방식입니다."

✅ **좋은 예**:
> "Chunk는 마치 뷔페에서 접시를 가득 채운 후에 한 번에 자리로 가는 것과 같습니다.
> 음식 하나 집고 자리 가고, 또 하나 집고 자리 가면 비효율적이잖아요?
> 그래서 '청크(덩어리)' 단위로 모아서 처리하는 겁니다."

### 5. 버그 섹션 작성법
```markdown
#### 버그: [에러 이름]

**증상**: 
[콘솔에 나타난 에러 메시지]

**원인**: 
[왜 이 에러가 발생했는지]

**해결**: 
[어떻게 고쳤는지 코드 포함]

**교훈**: 
[앞으로 어떻게 피할 수 있는지]
```

---

## 📝 문서 템플릿
```markdown
# FOR-BatchFlow-Step[번호]: [제목]

> [한 줄 요약]

---

## 🎬 Before We Start

[왜 배우는지, 비유로 설명]

---

## 🏗️ What We're Building

[결과물 미리보기, 구조 다이어그램]

---

## 🧠 Core Concepts

### [개념 1]
[비유 + 코드]

---

## 💻 Step-by-Step Implementation

### Step 1: [소제목]
[설명 + 코드]

---

## 🧪 Testing

[테스트 코드 + 실행 결과]

---

## 🐛 Lessons Learned

### 버그: [제목]
[증상 → 원인 → 해결 → 교훈]

### 시니어 개발자의 사고방식
[인사이트]

---

## 🎯 Key Takeaways

1. [포인트 1]
2. [포인트 2]
3. [포인트 3]

---

## 🔗 Next Steps

[다음 Step 예고]
```

---

## 언어 규칙

- **한국어 사용**: 모든 설명, 주석
- **영어 유지**: 기술 용어 (Chunk, Step, Job, Reader, Writer 등)
- **코드 주석**: 한국어 권장
```java
// ✅ 좋은 예
// 1년 이상 미접속 회원을 조회하는 Reader
public JpaPagingItemReader<Member> dormantMemberReader() { }

// ❌ 나쁜 예
// Reader for querying members who haven't logged in for over a year
```
```

---

## 3️⃣ 프롬프트 예시

### 예시 A: 기본 요청 (교육자료 자동 생성)
```
Step 09의 Chunk 기반 처리 모델을 구현해줘.

요구사항:
- JpaPagingItemReader로 회원 조회
- ItemProcessor에서 휴면 상태로 변경
- JpaItemWriter로 저장
- Chunk Size는 1000

@docs/skills/spring-batch-chunk.md 참고
```

> `CLAUDE.md`에 교육자료 규칙이 있으므로, AI가 자동으로 `FOR-BatchFlow-Step09.md`도 생성

---

### 예시 B: 교육자료 강조 요청
```
Step 15 휴면회원 전환 Job을 구현해줘.

@docs/skills/spring-batch-chunk.md 참고
@docs/skills/education-guide.md 참고하여 교육 가이드도 상세히 작성해줘

특히 교육자료에 다음 내용을 포함해줘:
- JPA N+1 문제가 발생할 수 있는 상황과 해결법
- fetchSize 튜닝 경험
- 실무에서 흔히 하는 실수
```

---

### 예시 C: 교육자료 생략 요청
```
Step 20 Skip 처리 로직 빠르게 프로토타이핑 해줘.
교육자료 생략.
```

> `CLAUDE.md`에 "생략" 명시 시 교육자료 안 만들도록 규칙이 있으므로 생략됨

---

### 예시 D: 교육자료만 별도 요청
```
Step 09가 이미 구현되어 있어.
@docs/skills/education-guide.md 참고해서
FOR-BatchFlow-Step09.md 교육 가이드만 작성해줘.

현재 코드:
[코드 붙여넣기]

실제로 마주친 버그:
- JpaPagingItemReader에서 ORDER BY 누락으로 데이터 중복 발생
- fetchSize와 chunkSize 불일치로 메모리 문제
```

---

## 최종 추천 구조
```
project/
├── CLAUDE.md                    # 간략한 규칙 + 스킬 참조
├── docs/
│   ├── skills/
│   │   ├── spring-batch-core.md
│   │   ├── spring-batch-chunk.md
│   │   ├── spring-batch-testing.md
│   │   └── education-guide.md    # 교육자료 작성 지침
│   │
│   └── guides/
│       ├── FOR-BatchFlow-Step01.md
│       ├── FOR-BatchFlow-Step02.md
│       └── ...