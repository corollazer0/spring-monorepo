# TestCraft 실행 가이드 (spring-test-onboarding)

> 테스트 코드가 처음인 분을 위한 **완전 초보자용** 가이드입니다.
> 그대로 따라하면 10분 안에 테스트가 돌고, 웹앱이 브라우저에 뜹니다.
> 무엇을 배우는 과정인지는 [커리큘럼 문서](../docs/test/curriculum/00-TestCraft-Curriculum.md)를 보세요.

---

## 0. 준비물

| 필요한 것 | 확인 방법 | 없다면 |
|----------|----------|--------|
| JDK 8 이상 (11 권장) | 터미널에서 `java -version` | [Temurin JDK 다운로드](https://adoptium.net/) |
| Git | `git --version` | [git-scm.com](https://git-scm.com/) |
| IDE (IntelliJ 권장) | - | IntelliJ Community(무료)면 충분 |

> Gradle은 설치할 필요 없습니다 — 레포에 포함된 `gradlew`(Gradle Wrapper)가 알아서 받아줍니다.

```bash
git clone https://github.com/corollazer0/spring-monorepo.git
cd spring-monorepo
```

⚠️ 아래 모든 명령은 **레포 최상위 폴더(spring-monorepo)** 에서 실행합니다.
Windows는 `.\gradlew`, Mac/Linux는 `./gradlew` 입니다.

---

## 1. 30초 검증 — 테스트가 도는지부터 확인

```bash
.\gradlew :spring-test-onboarding:test
```

처음 한 번은 의존성 다운로드로 수 분 걸릴 수 있습니다. 마지막에 이렇게 나오면 성공:

```
BUILD SUCCESSFUL
```

> 일부 테스트가 **skipped**로 표시되는 것은 정상입니다 — 여러분이 풀어야 할
> 연습문제(`exercise` 패키지, `@Disabled` 상태)이기 때문입니다.

테스트 결과를 눈으로 보고 싶다면 브라우저로 여세요:
`spring-test-onboarding/build/reports/tests/test/index.html`

---

## 2. 웹앱 실행 — 눈으로 보기

```bash
.\gradlew :spring-test-onboarding:bootRun
```

콘솔에 `Started TestOnboardingApplication` 이 보이면 브라우저에서:

| 화면 | 주소 |
|------|------|
| 글 목록 | http://localhost:8080/posts |
| 로그인 | http://localhost:8080/login |
| 회원가입 | http://localhost:8080/signup |

**시드 계정** (비밀번호는 모두 `spring123!`):

| 아이디 | 권한 |
|--------|------|
| `writer1`, `writer2` | USER |
| `admin` | ADMIN |

### 직접 해볼 것 (3분 코스)

1. 글 목록에서 아무 글이나 클릭 → 상세 + 댓글 확인
2. 로그인 없이 우측 상단 **글쓰기** 클릭 → 로그인 페이지로 안내되는 것 확인 (보안!)
3. `writer1 / spring123!` 로그인 → 글쓰기 → 등록 → 내 글이 목록에 뜨는 것 확인
4. 제목을 비우고 등록 → 에러 메시지와 함께 폼이 유지되는 것 확인 (검증!)

**종료**: 터미널에서 `Ctrl + C`

> 💾 DB는 인메모리 H2라서 **앱을 끄면 데이터가 초기화**됩니다(시드 데이터로 리셋). 정상입니다.

### (선택) DB 내부 들여다보기 — H2 콘솔

앱 실행 중에 http://localhost:8080/h2-console 접속:

| 항목 | 값 |
|------|-----|
| JDBC URL | `jdbc:h2:mem:testdb` |
| User Name | `sa` |
| Password | (비움) |

`SELECT * FROM MEMBER;` 를 실행해보세요 — 비밀번호가 어떻게 저장되는지도 볼 수 있습니다.

---

## 3. IntelliJ로 열기 (학습은 IDE에서)

1. IntelliJ → **Open** → `spring-monorepo` 폴더 선택 (모듈 폴더가 아니라 **최상위**!)
2. 우측 하단 "Gradle 동기화" 완료까지 대기 (첫 회 수 분)
3. 테스트 실행: 테스트 파일을 열고 클래스/메서드 옆 **초록 ▶ 버튼** 클릭
   - 예: `src/test/java/com/testonboarding/step01/example/PasswordPolicyValidatorTest.java`
4. 앱 실행: `TestOnboardingApplication.java` 옆 초록 ▶ 버튼

> 한글 주석이 깨져 보이면: Settings → Editor → File Encodings → 전부 **UTF-8** 로.

---

## 4. 학습 시작하기

```
1) docs/test/curriculum/00-TestCraft-Curriculum.md   ← 전체 지도 (여기서 시작!)
2) docs/test/education/FOR-Test-Step01.md            ← Step 1 문서 읽기
3) step01/example 테스트 실행하며 따라잡기
4) step01/exercise 의 @Disabled 지우고 TODO 풀기
5) step01/answer 와 비교 → 다음 Step으로
```

특정 Step의 테스트만 돌리기:

```bash
.\gradlew :spring-test-onboarding:test --tests "com.testonboarding.step01.*"
```

---

## 5. 자주 막히는 것 (FAQ)

| 증상 | 해결 |
|------|------|
| `Port 8080 was already in use` | 이미 떠 있는 앱 종료(이전 터미널 Ctrl+C) 또는 8080 점유 프로세스 종료 |
| `JAVA_HOME is not set` | JDK 설치 후 환경변수 JAVA_HOME 설정, 터미널 재시작 |
| `gradlew: command not found` | 레포 최상위 폴더인지 확인, Windows는 `.\gradlew` |
| 콘솔 한글이 깨짐 | 표시 문제일 뿐(코드페이지) — 테스트 결과(HTML 리포트)는 정상 |
| 테스트 27개가 skipped | 정상! 여러분이 풀 exercise입니다 |
| 그 외 이상 동작 | `docs/test/skills/spring-test-annotations.md`의 **단골 미스터리 진단표** 참고 |

---

## 6. 더 보기

| 문서 | 내용 |
|------|------|
| [커리큘럼](../docs/test/curriculum/00-TestCraft-Curriculum.md) | 학습 철학, Step 1~12 지도, 진행 루틴 |
| [치트시트](../docs/test/skills/spring-test-annotations.md) | 언제 무엇을 쓰나 + 진단표 |
| [모듈 규칙](./CLAUDE.md) | 코드/테스트 작성 규약 (AI 협업 포함) |
