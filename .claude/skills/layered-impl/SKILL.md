---
name: layered-impl
description: Spring 레이어드 아키텍처로 기능을 구현할 때 사용. Controller(REST/뷰)/Service/DAO/DTO의 책임 분리 표준, 예외·에러응답 규약, 로깅 규칙, Java 8 제약을 정의한다.
---

# 레이어드 구현 표준

## 1. 레이어별 책임 — 섞지 마라

| 레이어 | 책임 | 금지 |
|--------|------|------|
| Controller (@RestController) | URL 매핑, 바인딩, @Valid, 상태코드(201+Location/204), Principal 추출 | 비즈니스 판단, DB 접근 |
| ViewController (@Controller) | 뷰 이름/Model, 폼 흐름(@ModelAttribute+BindingResult, 실패 시 폼 재표시, 성공 시 PRG redirect) | JSON 응답, 비즈니스 판단 |
| Service | 비즈니스 규칙(존재 검증, 소유권 검증, 정책), 트랜잭션 경계 | HTTP 개념(상태코드/요청객체) 침투 |
| DAO (@Mapper) | SQL 실행만 — 인터페이스 + XML | 비즈니스 분기 |
| DTO | 요청(검증 어노테이션)/응답(static from(domain)) 분리, 도메인 직접 노출 금지 | 응답 DTO에 비밀번호 등 민감정보 |
| 외부 API Client (`external/{대상}/`) | 외부 호출 + 번역(타임아웃·재시도, 상태코드→우리 의미, 기술 예외→우리 예외), 외부 계약 전용 DTO | 도메인/내부 DTO를 외부에 직접 노출, 비즈니스 판단 |

핵심 규칙: **데이터 소유권 검증("작성자 본인만")은 Service의 책임** — Security(URL 인가)가 아니다.

## 2. 예외 & 에러 응답 규약

- 비즈니스 예외는 `BusinessException` 계열로 상속 정의, 메시지에 식별자 포함
- `@RestControllerAdvice`가 예외→상태코드 번역: 검증실패 400+fieldErrors / NotFound 404 / 소유권 403 / 중복 409 / 그 외 500(**내부 메시지 은닉, 로그에만**)
- 에러 응답은 단일 규약 객체(ErrorResponse: status/message/fieldErrors)로 통일
- 화면(폼) 흐름의 비즈니스 예외는 컨트롤러에서 잡아 `bindingResult.reject()`로 폼 재표시

## 3. 형식 검증 vs 비즈니스 검증

- 형식(필수/길이/패턴) → DTO의 Bean Validation (`javax.validation`)
- DB를 봐야 아는 검증(중복 등) → Service
- Controller에 `@Valid` 누락 주의 — 어노테이션 선언만으로는 동작하지 않는다

## 4. 로깅 / Java 8 제약 (이 조직 공통)

- 로그 접두사 `>>>>>`, 에러 로그는 예외 객체 포함: `log.error(">>>>> [ERROR] ...: {}", e.getMessage(), e)`
- Java 8 전용: `var`/`record`/Text Block/`Stream.toList()`/switch expression/`jakarta.*` **금지**, `javax.*` 사용
- 버전 업그레이드 금지 — BOM이 관리하는 버전을 덮어쓰지 않는다 (모듈 CLAUDE.md의 고정표 참조)

> 이 레포의 모범 구현: `spring-test-onboarding`의 `com.testonboarding` (board/member/comment/web — 보안 포함),
> `spring-web-onboarding`의 `com.webflow` (external 클라이언트 계층, 파일/캐시/스케줄 — 실무 API 종합).
