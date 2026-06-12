# 공통 라이브러리 로컬 개발 가이드 — Gradle Composite Build

> 공통 jar(예: NCDPcommon)와 소비 MSA 프로젝트가 **별도 레포**일 때,
> "한 줄 수정 → publish → 버전 변경 → sync → 기동"의 느린 루프를
> "한 줄 수정 → 소비 프로젝트에서 바로 실행"으로 바꾸는 방법.

---

## 1. 문제와 원리

### Before (현재 루프 — 수정 1회당 수십 분)

```
NCDPcommon 수정 → Nexus에 publish → MSA 프로젝트 SNAPSHOT 버전 변경
  → gradle sync → 앱 기동 → 눈으로 확인 → (틀렸으면 처음부터)
```

### After (composite build — 수정 1회당 수 초~수 분)

```
NCDPcommon 수정 → MSA 프로젝트에서 gradlew test (또는 bootRun)
  → Gradle이 Nexus 좌표를 "로컬 NCDPcommon 소스"로 자동 치환해 함께 빌드
```

**원리**: `includeBuild`로 두 독립 빌드를 합성하면, 소비 프로젝트의
`implementation 'com.회사:ncdp-common:1.2.3-SNAPSHOT'` 선언이
**좌표(group:artifact)가 일치하는** 로컬 빌드 산출물로 대체된다.
publish도, 버전 변경도, sync도 필요 없다 — 소스가 곧 의존성이 된다.

## 2. 전제: 나란히 클론 (멀티 레포 컨벤션)

별도 레포이므로 로컬 배치 컨벤션 하나만 정한다:

```
C:\work\
├── NCDPcommon\        ← 공통 레포 클론
├── msa-order\         ← 소비 레포 클론 (형제 폴더!)
└── msa-member\
```

상대 경로(`../NCDPcommon`)가 전 팀원에게 동일해진다 — 절대 경로 커밋 금지의 전제.

## 3. 적용 방법 (둘 중 택일 — A부터 시작 권장)

### 방법 A. 플래그 방식 — 아무것도 커밋하지 않는다 (개인용, 권장 출발점)

소비 프로젝트에서:

```bash
# 공통 수정분을 물고 테스트
gradlew test --include-build ..\NCDPcommon

# 공통 수정분을 물고 기동
gradlew bootRun --include-build ..\NCDPcommon
```

끝. 레포에 아무 변경이 없으므로 도입 리스크 0 — **오늘 바로 시도해볼 수 있는 방법.**

### 방법 B. 스위치 방식 — 팀과 공유하는 opt-in (방법 A가 익숙해진 뒤)

소비 프로젝트의 `settings.gradle` 끝에:

```groovy
// 공통 라이브러리 로컬 개발 모드 — gradle.properties에 ncdpLocal=true 일 때만
// (기본 OFF — CI와 일반 팀원에겐 아무 영향 없다)
if (providers.gradleProperty('ncdpLocal').getOrElse('false') == 'true') {
    includeBuild('../NCDPcommon')
}
```

공통 담당자만 자기 `~/.gradle/gradle.properties`(개인 전역)에 `ncdpLocal=true` —
플래그 없이 항상 로컬 모드, 다른 사람과 CI는 Nexus 버전 그대로.

## 4. 치환이 진짜 됐는지 확인하는 법 (중요!)

좌표가 안 맞으면 **에러 없이 조용히 Nexus 버전을 쓴다** — 가장 위험한 침묵.
반드시 한 번 확인:

```bash
gradlew :app:dependencies --configuration runtimeClasspath | findstr ncdp
# 치환 성공:  com.회사:ncdp-common:1.2.3-SNAPSHOT -> project :ncdp-common   ← "-> project"!
# 치환 실패:  com.회사:ncdp-common:1.2.3-SNAPSHOT                            ← 그대로면 Nexus
```

치환 실패 시 1순위 점검: NCDPcommon `build.gradle`의 `group`과 산출물 이름이
소비 선언의 좌표와 **정확히 일치**하는가. (불일치를 못 고치는 사정이 있으면
`includeBuild(...) { dependencySubstitution { ... } }`로 수동 매핑)

## 5. 함정 진단표

| 증상 | 1순위 의심 |
|------|-----------|
| 수정했는데 반영이 안 된다 | 치환 실패(조용히 Nexus 사용) — 4장 확인법으로 "-> project" 검사 |
| composite 빌드 자체가 실패 | 두 레포의 Gradle 래퍼/플러그인 버전 충돌 — 소비 쪽 래퍼로 통일 (composite는 루트 빌드의 Gradle로 돈다) |
| IntelliJ에서 공통 코드로 점프가 안 됨 | Gradle 탭 우클릭 → Composite Build Configuration에서 NCDPcommon 체크 (또는 런 컨피그에 --include-build) |
| CI가 갑자기 로컬 경로를 찾음 | 방법 B의 스위치가 프로젝트 gradle.properties에 커밋됨 — 개인 전역(~/.gradle)에만 둘 것 |
| 공통이 멀티모듈인데 일부만 치환 | 정상 — 모듈별 좌표 매칭. 안 되는 모듈의 group/이름 확인 |

## 6. 역할 분담 — composite가 대체하는 것 / 못 하는 것

| | 도구 |
|---|---|
| 개발 중 빠른 피드백 | ✅ composite build (이 문서) |
| 수정 자체의 1차 검증 | ❌ composite의 일이 아님 — **공통 모듈 자체 테스트** ([이식 가이드 8장](./CLAUDE-PORTING-GUIDE.md)) |
| 팀 배포/공식 버전 | ❌ 기존 publish 파이프라인 그대로 — composite는 **개발 루프 전용**, 배포를 대체하지 않는다 |
| 머지 전 "소비자 안 깨짐" 보증 | CI downstream 검증 잡 (이식 가이드 8장) |

> **운영 원칙 한 줄**: 로컬 개발은 composite로, 검증은 공통 모듈 테스트로,
> 배포는 기존 파이프라인으로 — 셋을 섞지 않는 것이 규율이다.
