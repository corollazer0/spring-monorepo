package com.testonboarding.advanced.step13.answer;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * [심화 Step 13 — answer] ArchRulesExerciseTest 모범답안
 *
 * 채점 포인트: TODO 3 — "Spring 비의존" 규칙의 의미를 이해했는가.
 * 이 규칙이 그린인 한, policy는 구조적으로 순수 단위 테스트 가능성이 보장된다 —
 * Step 1에서 누린 편안함이 우연이 아니라 봉인된 설계가 된다.
 */
@DisplayName("아키텍처 규칙 (모범답안)")
class ArchRulesAnswerTest {

    private static JavaClasses productionClasses;

    @BeforeAll
    static void importClasses() {
        productionClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.testonboarding");
    }

    @Test
    @DisplayName("DTO는 아래층(service/dao)도 위층(controller)도 모른다 — 순수한 운반책")
    void dto는_계층클래스_의존금지() {
        // (TODO 1 답)
        noClasses().that().resideInAPackage("..dto..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("..service..", "..controller..", "..dao..")
                .because("DTO가 계층을 알기 시작하면 운반책이 아니라 비즈니스 객체가 된다")
                .check(productionClasses);
    }

    @Test
    @DisplayName("policy 클래스는 *Validator로 끝나고, Spring을 모른다 (Step 1이 가능한 이유)")
    void policy는_순수_Validator() {
        // (TODO 2 답)
        classes().that().resideInAPackage("..policy..")
                .should().haveSimpleNameEndingWith("Validator")
                .check(productionClasses);

        // (TODO 3 답) : 이 그린이 유지되는 한 정책 검증은 영원히 new로 테스트된다
        noClasses().that().resideInAPackage("..policy..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .because("정책은 순수 자바 — 프레임워크 없는 ms 단위 테스트의 구조적 보장")
                .check(productionClasses);
    }
}
