# [Web Step 5] 파일 업로드/다운로드 — JSON이 아닌 입력

> **소요 시간**: 약 1.5시간
> **이번 Step의 도구**: 🆕 MultipartFile, MockMultipartFile, @TempDir, 확장자 화이트리스트, 경로 탈출 방어, Resource 응답
> **코드 위치**: `spring-web-onboarding/src/{main/java/com/webflow/file, test/java/com/webflow/step05}/`

---

## 1. Before We Start — "이미지는 어떻게 올리죠?"

지금까지의 입력은 전부 JSON이었습니다. 파일은 다릅니다 —
**multipart/form-data**라는 별도 규격으로 오고, 받으면 디스크 어딘가에
저장해야 하고, 그 순간 새로운 질문들이 따라옵니다:

- 아무 파일이나 받아도 되나? (`악성코드.exe`가 올라오면?)
- 사용자가 보낸 파일명을 그대로 써도 되나? (`../../etc/passwd`라면?)
- 테스트는 진짜 디스크에 써야 하나? (어디에? 다 돌고 나면 누가 치우나?)

업로드는 기능이기 이전에 **공격 표면**입니다. 이번 Step의 절반은 보안입니다.

## 2. What We're Building

```
POST /api/products/{id}/image  (multipart, 파트명 image)
  → 검증(확장자 화이트리스트) → UUID 이름으로 저장 → product.image_path 기록
GET  /api/products/{id}/image
  → Resource 응답 + Content-Disposition: attachment
```

```
src/main/java/com/webflow/file/FileStorageService.java  ← 검증+저장+읽기 (보안 3원칙)
src/test/java/com/webflow/step05/
├── example/FileStorageServiceTest.java  ← 순수 단위 + @TempDir (Spring 없음!)
├── example/ProductImageApiTest.java     ← multipart/Resource HTTP 계약
├── exercise/FileValidationExerciseTest.java ← jpeg/이중 확장자/왕복
└── answer/FileValidationAnswerTest.java
```

## 3. Core Concepts

### 3-1. 🆕 업로드 보안 3원칙

```java
// ① 화이트리스트 — "안전한 것만 허용" (블랙리스트는 늘 한 발 늦는다)
private static final Set<String> ALLOWED_EXTENSIONS = ... ("jpg", "jpeg", "png");

// ② 저장 이름은 서버가 생성 — 사용자 입력이 경로가 되는 일은 없다
String storedName = UUID.randomUUID() + "." + extension;

// ③ 읽기 경로는 경계 검증 — ../ 탈출 차단
Path target = uploadDir.resolve(filename).normalize();
if (!target.startsWith(uploadDir)) throw new InvalidFileException(...);
```

①에서 "exe만 막자"(블랙리스트)는 실패하는 접근입니다 — jsp, html, svg, bat...
위험한 확장자는 끝이 없습니다. Step 2의 정렬 화이트리스트와 같은 철학:
**허용 목록에 없으면 거부**.

②는 두 가지를 한 번에 해결합니다 — 파일명 충돌(같은 photo.jpg 두 명이 올리면?)과
파일명 인젝션(이름에 `../`나 널문자가 들어있다면?). 원본 이름은 로그에만 남깁니다.

③은 다운로드 쪽 방어입니다. `normalize()`로 `../`를 풀어낸 **최종 경로**가
업로드 폴더 안인지 확인 — 문자열 검사(`contains("..")`)보다 강한, 경로 기반 검증.

### 3-2. 🆕 @TempDir — 파일 테스트의 격리

```java
@TempDir
Path tempDir;   // 테스트마다 새로 생성, 끝나면 자동 삭제

fileStorageService = new FileStorageService(tempDir.toString());
```

`FileStorageService`가 저장 경로를 **생성자(프로퍼티)로 받는** 평범한 클래스라서
가능한 일입니다 — Spring 없이 `new` 해서 진짜 파일 I/O를 검증합니다.
경로를 클래스 안에 하드코딩했다면? 테스트가 실제 ./uploads를 더럽히고,
두 번 돌리면 결과가 달라졌을 겁니다. **설정 주입 = 테스트 가능성**, RestTemplateBuilder
(Step 3)와 같은 원리가 파일에서 반복됩니다.

업로드 파일 흉내는 `MockMultipartFile(파트명, 원본이름, 컨텐츠타입, 바이트)` —
HTTP 없이 MultipartFile 인터페이스를 채워주는 테스트 더블입니다.

### 3-3. multipart와 Resource — MockMvc의 두 가지 새 모양

```java
// 업로드: post()가 아니라 multipart()
mockMvc.perform(multipart("/api/products/1/image").file(mockFile))

// 다운로드: 본문이 JSON이 아니라 바이트
.andExpect(header().string(CONTENT_DISPOSITION, containsString("attachment")))
.andExpect(content().bytes(expectedBytes));
```

`Content-Disposition: attachment; filename=...`가 브라우저의 "다른 이름으로 저장"
동작을 만드는 헤더입니다 — 없으면 브라우저가 화면에 표시하려 시도합니다(inline).
이 헤더도 **계약**이므로 테스트로 봉인합니다.

### 3-4. 검증 실패 시 부수효과 없음 — 거부의 완결성

exe 거부 테스트의 마지막 단언을 보세요:

```java
assertThat(tempDir.toFile().listFiles()).isEmpty();   // 디스크에 아무것도 없다
```

"예외가 터졌다"만 확인하면 반쪽입니다. **거부했는데 파일이 남아있다면**(검증 전에
저장부터 했다면) 디스크가 거부된 쓰레기로 차오릅니다. 예외 + 부수효과 부재,
두 단언이 합쳐져야 "검증이 쓰기보다 먼저"라는 구현 순서가 봉인됩니다 —
Step 3에서 never()로 했던 일의 파일 버전.

### 3-5. 고아 파일 방지 — 존재 확인이 저장보다 먼저

`uploadProductImage`는 상품 존재 확인 → 저장 → 경로 기록 순서입니다.
저장부터 하고 상품이 없으면? 디스크에 **주인 없는 파일**(고아)이 남습니다.
DB는 롤백이 되지만 **파일 시스템은 롤백이 없습니다** — 그래서 파일 쓰기는
가장 마지막 직전, 실패할 수 있는 검증들은 전부 그 앞에 둡니다.

## 4. Step-by-Step

```bash
.\gradlew :spring-web-onboarding:test --tests "com.webflow.step05.*"
```

1. `FileStorageService` — 보안 3원칙이 코드 어디에 있는지 짚기
2. `FileStorageServiceTest` — Spring 없는 파일 테스트 (@TempDir + new)
3. `ProductImageApiTest` — multipart()와 content().bytes()
4. **일부러 깨뜨려보기**: store()에서 확장자 검증을 Files.copy **뒤로** 옮기면
   어떤 테스트가 깨질까? (listFiles().isEmpty() — 부수효과 단언의 존재 이유)

## 5. Testing — exercise 풀기

`step05/exercise/FileValidationExerciseTest.java`의 TODO 1~5를 채우세요.
TODO 3(이중 확장자 `photo.jpg.exe`)이 생각할 거리입니다 — 마지막 점 기준으로
exe로 판정되어 **거부되는 것이 올바른 동작**입니다. 공격자는 늘 경계값을 노립니다.

## 6. Lessons Learned

### 사례: 프로필 이미지로 올라온 웹쉘

- **증상**: 게시판 이미지 업로드로 .jsp 파일이 올라가 서버에서 실행됨 (서버 장악)
- **원인**: 확장자 검증 없음 + 업로드 폴더가 웹 루트 안 + 사용자 파일명 그대로 저장
- **해결**: 화이트리스트 + 서버 생성 파일명 + 웹 루트 밖 저장 (3원칙 전부)
- **교훈**: 업로드는 "사용자가 서버 디스크에 쓰기를 한다"는 뜻이다. 기능이 아니라
  공격 표면으로 먼저 보라.

### 사례: 테스트 돌릴 때마다 쌓이는 uploads 폴더

- **증상**: CI 서버 디스크 풀 — 원인은 수만 개의 테스트 업로드 파일
- **원인**: 테스트가 실제 업로드 경로에 쓰고 정리하지 않음
- **해결**: @TempDir — 테스트마다 격리된 폴더, 종료 시 자동 삭제
- **교훈**: 파일을 만드는 테스트는 "누가 치우나"까지가 테스트 설계다.

### 시니어의 시선

> 업로드 코드리뷰 체크리스트: ① 화이트리스트인가 블랙리스트인가 ② 저장 파일명에
> 사용자 입력이 들어가는가 ③ 읽기 경로에 경계 검증이 있는가 ④ 크기 제한은
> (Boot 기본 1MB — spring.servlet.multipart.max-file-size) ⑤ 거부 시 부수효과는.
> 다섯 중 하나라도 비면 보안 리뷰로 넘깁니다.

## 7. Key Takeaways

- 업로드 보안 3원칙: 확장자 화이트리스트 / 서버 생성 파일명 / 경로 경계 검증
- @TempDir + 생성자 주입 = Spring 없이 진짜 파일 I/O 테스트 (자동 정리)
- MockMultipartFile로 업로드, multipart()로 요청, content().bytes()로 다운로드 검증
- 거부 = 예외 + 부수효과 없음, 두 단언이 한 세트
- 파일 시스템은 롤백이 없다 — 쓰기는 모든 검증 뒤에 (고아 파일 방지)

## 8. Next Steps — 다음 Step의 문제

상품 목록 API가 느려지기 시작했습니다. 모니터링을 보니 **같은 인기 상품 조회가
초당 수백 번** — 매번 똑같은 SELECT가 DB를 두드립니다. 상품 정보는 분 단위로도
잘 안 바뀌는데요.

"한 번 읽은 걸 기억해두면 되잖아?" — 맞습니다, 캐싱입니다. 그런데:

> 기억해둔 값은 언제까지 유효하죠? 상품이 수정되면? 주문으로 재고가 바뀌면?

캐시의 진짜 어려움은 저장이 아니라 **무효화**입니다.
**Step 6: 캐싱**에서 @Cacheable/@CacheEvict와 "DB가 몇 번 불렸나" 검증법을 다룹니다.
