package com.testonboarding.advanced.step13.example;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * [심화 Step 13 — example] 아키텍처를 테스트로 봉인한다 — ArchUnit
 *
 * 지금까지의 테스트는 "동작"을 봉인했다 (이 입력이면 이 출력).
 * ArchUnit은 "구조"를 봉인한다 (이 패키지는 저 패키지를 모른다) —
 * 코드리뷰에서 사람이 매번 잡던 규칙을, 빌드가 잡게 만든다.
 *
 * 동작 원리: 바이트코드를 읽어(JavaClasses) 의존 그래프를 만들고,
 * 선언한 규칙(ArchRule)을 check한다. Spring 컨텍스트도, 실행도 필요 없다 —
 * 순수 단위 테스트만큼 빠르고 결정적이다.
 *
 * import는 비싸다(전체 클래스 스캔) → @BeforeAll에서 한 번만 (컨텍스트 캐싱과 같은 발상).
 * DoNotIncludeTests: 검증 대상은 프로덕션 구조다 — 테스트 코드는 제외.
 */
@DisplayName("아키텍처 규칙")
class ArchitectureRulesTest {

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.testonboarding");
    }

    @Nested
    @DisplayName("계층 의존 규칙 — 물은 위에서 아래로만 흐른다")
    class LayerRules {

        @Test
        @DisplayName("Controller는 DAO를 직접 부르지 않는다 (Service를 건너뛰는 지름길 금지)")
        void controller는_dao_직접의존_금지() {
            // 이 규칙이 깨지는 순간: 트랜잭션 경계·비즈니스 검증(소유자 확인 등)이 증발한다
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..dao..")
                    .because("Controller→Service→DAO 단방향 — 지름길은 검증과 트랜잭션을 건너뛴다")
                    .check(productionClasses);
        }

        @Test
        @DisplayName("Service는 Controller를 모른다 (역류 금지 — 아래층은 위층의 존재를 모른다)")
        void service는_controller_역방향의존_금지() {
            noClasses().that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..")
                    .because("역방향 의존은 순환의 시작 — 아래층이 위층을 알면 계층이 무너진다")
                    .check(productionClasses);
        }

        @Test
        @DisplayName("도메인은 Spring을 모른다 — 순수 자바라서 new로 테스트된다 (Step 1의 구조적 보장)")
        void domain은_spring_비의존() {
            // Step 1에서 PasswordPolicyValidator를 new로 테스트할 수 있었던 건 우연이 아니라 구조다
            noClasses().that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .because("프레임워크에 물든 도메인은 순수 단위 테스트 가능성을 잃는다")
                    .check(productionClasses);
        }
    }

    @Nested
    @DisplayName("모노레포 헌법 — 모듈 격리")
    class ModuleIsolationRules {

        @Test
        @DisplayName("다른 모듈(batchflow/webflow)을 import하지 않는다 — 루트 CLAUDE.md의 기계화")
        void 타모듈_상호참조_금지() {
            // 사람이 지키던 헌법 1조가 이제 빌드가 지키는 규칙이 된다
            noClasses().should().dependOnClassesThat()
                    .resideInAnyPackage("com.batchflow..", "com.webflow..")
                    .because("모듈 격리는 이 레포의 헌법 — 어기면 빌드가 빨갛게 알려준다")
                    .check(productionClasses);
        }
    }

    @Nested
    @DisplayName("네이밍·위치 규약 — 이름이 곧 주소다")
    class ConventionRules {

        @Test
        @DisplayName("controller 패키지의 클래스는 *Controller로 끝난다 (dao/service도 동일 규칙)")
        void 계층별_네이밍_규약() {
            classes().that().resideInAPackage("..controller..")
                    .should().haveSimpleNameEndingWith("Controller")
                    .check(productionClasses);
            classes().that().resideInAPackage("..service..")
                    .should().haveSimpleNameEndingWith("Service")
                    .check(productionClasses);
            classes().that().resideInAPackage("..dao..")
                    .should().haveSimpleNameEndingWith("Dao")
                    .check(productionClasses);
        }

        @Test
        @DisplayName("DAO는 인터페이스다 — 구현은 MyBatis XML의 몫")
        void dao는_인터페이스() {
            classes().that().resideInAPackage("..dao..")
                    .should().beInterfaces()
                    .because("DAO에 구현 클래스가 생기는 순간 XML 매퍼 규약이 흔들린다")
                    .check(productionClasses);
        }

        @Test
        @DisplayName("예외는 전부 common.exception에 산다 + RuntimeException 계열이다")
        void 예외_위치와_계열_규약() {
            classes().that().haveSimpleNameEndingWith("Exception")
                    .should().resideInAPackage("..common.exception..")
                    .because("예외가 흩어지면 GlobalExceptionHandler가 전모를 못 본다")
                    .check(productionClasses);
            classes().that().resideInAPackage("..common.exception..")
                    .and().haveSimpleNameEndingWith("Exception")
                    .should().beAssignableTo(RuntimeException.class)
                    .because("체크 예외는 계층마다 throws를 강요한다 — 이 레포는 런타임 계열만")
                    .check(productionClasses);
        }
    }
}
