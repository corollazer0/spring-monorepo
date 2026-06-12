---
name: spring-troubleshoot
description: Spring 애플리케이션/테스트의 원인 모를 동작(401/403, 인코딩 깨짐, 테스트 비결정성, 빌드 급감속, SQL 오류 등)을 진단할 때 사용. 단골 원인 진단표와 표준 진단 절차를 제공한다.
---

# Spring 트러블슈팅 표준

## 1. 진단 절차 (순서를 지켜라)

1. **증상을 정확히** — 에러 전문, 상태코드, "언제부터/무엇을 바꾼 뒤" 확보
2. **단골 진단표 대조** (아래 2장) — 절반은 여기서 끝난다
3. **최소 재현** — 가능하면 실패를 재현하는 가장 작은 테스트를 만든다
4. **이분 탐색** — 변경 묶음을 절반씩 되돌려 원인 커밋/설정을 좁힌다
5. 원인은 **증거(파일:라인/로그)** 와 함께 보고, 수정은 별도 단계로

## 2. 단골 원인 진단표

### 보안 (401/403)
| 증상 | 1순위 의심 |
|------|-----------|
| permitAll인데 401 | 슬라이스 테스트에 SecurityConfig 미로드 (@Import 누락) |
| 로그인했는데 403 | CSRF 토큰 누락 — CSRF 검증이 인증보다 먼저 |
| 화면 미인증이 302가 아닌 401 | 요청에 Accept: text/html 없음 (브라우저로 인식 안 됨) |
| 토큰 발급은 되는데 전부 401 | Bearer 접두사/헤더명 오타, 필터 체인 미등록(@Order/antMatcher) |

### 데이터/SQL
| 증상 | 1순위 의심 |
|------|-----------|
| 방언 SQL이 테스트에서 문법 오류 | @MybatisTest의 DataSource 교체 — Replace.NONE 누락 |
| 한글이 ???/깨짐 | 인코딩: compile encoding, `spring.sql.init.encoding`, 콘솔 코드페이지 (Windows cp949) |
| 페이징 순서가 실행마다 다름 | ORDER BY 컬럼 동률 — PK 포함해 유일성 보장 |
| enum 매핑 깨짐 | enum 상수명 변경 vs DB 저장 문자열 불일치 |

### 테스트 비결정성/속도
| 증상 | 1순위 의심 |
|------|-----------|
| 혼자는 통과, 같이 돌리면 실패 | ThreadLocal(SecurityContextHolder/MDC) 미정리, 공유 인메모리 DB에 커밋된 잔여 데이터 |
| E2E 첫 실행만 통과 | RANDOM_PORT 롤백 불가 + AFTER_TEST_METHOD 정리 누락 |
| 빌드 급감속 | @MockBean 조합 차이로 컨텍스트 캐시 무효화 — 기동 횟수를 로그에서 세어보라 |
| UnnecessaryStubbingException | 그 테스트에서 안 일어나는 stubbing — 시나리오 오해 신호 (lenient로 끄지 말 것) |
| REST Docs "urlTemplate not found" | MockMvcRequestBuilders 사용 — pathParameters는 RestDocumentationRequestBuilders 필수 (static import 함정) |
| REST Docs 필드 누락/잉여 실패 | 문서와 페이로드 불일치(설계된 동작!) — 스텁 응답의 null 필드도 의심 (JSON에서 빠짐) |

### 배치 (Spring Batch)
| 증상 | 1순위 의심 |
|------|-----------|
| JobInstanceAlreadyCompleteException | 같은 파라미터로 성공한 Job 재실행(설계!) — 테스트는 removeJobExecutions 또는 유니크 파라미터 |
| JobOperator가 NoSuchJobException (Job 빈은 있는데) | JobRegistryBeanPostProcessor 미등록 — 컨텍스트의 빈 ≠ 레지스트리의 등록 |
| 재시작이 "이어서"가 아니라 처음부터 | restart(실행ID) 대신 새 파라미터 start를 함 / saveState(false)·상태 전이 설계 확인 |
| AsyncItemProcessor가 import 안 됨 | spring-batch-core가 아니라 spring-batch-integration 모듈 의존성 누락 |
| JobLauncherTestUtils 주입 실패 (Job 빈 모호) | 컨텍스트에 Job 빈 2개 이상 — JobLauncher 직접 사용 + 유니크 파라미터로 전환 |

### 환경 (Windows)
| 증상 | 1순위 의심 |
|------|-----------|
| git commit -m 이 pathspec 오류 | PowerShell 5.1이 인자 내 큰따옴표를 깨뜨림 — 메시지에 " 금지 |
| 빌드는 되는데 한글 출력 깨짐 | 콘솔 코드페이지(표시 문제) vs 실제 데이터 깨짐(인코딩 설정)을 구분하라 |

## 3. 원칙

- 패턴이 비슷하다고 단정하지 마라 — 같은 증상, 다른 원인이 흔하다. 진단표는 "1순위 가설"일 뿐
- 고치기 전에 "왜 지금까지는 됐는가"를 설명할 수 있어야 진짜 원인이다
- 해결한 새 함정은 이 진단표(및 레포 치트시트)에 추가하라 — 진단표는 자산이다
