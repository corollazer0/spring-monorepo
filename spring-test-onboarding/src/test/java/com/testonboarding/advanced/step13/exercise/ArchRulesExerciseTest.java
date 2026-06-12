package com.testonboarding.advanced.step13.exercise;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * [심화 Step 13 — exercise] 우리 팀의 규칙을 직접 봉인해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (힌트: example의 ArchitectureRulesTest — noClasses()/classes() 빌더)
 * 3. .\gradlew :spring-test-onboarding:test 로 통과를 확인한다
 *
 * 팁: 규칙을 쓰기 전에 "이 규칙이 지금 코드에서 성립하는지"부터 확인하라 —
 * 성립하지 않는 규칙을 봉인하면 빌드가 영원히 빨갛다. (ArchUnit 도입의 현실 순서:
 * 현황 측정 → 위반 정리 → 규칙 봉인)
 */
@Disabled("과제: docs/test/education/FOR-Test-Step13.md 참고 후 @Disabled를 제거하고 완성하세요")
@DisplayName("아키텍처 규칙 (연습문제)")
class ArchRulesExerciseTest {

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
        // TODO 1 : noClasses()로 "..dto.. 패키지의 클래스가
        //          ..service.. / ..controller.. / ..dao.. 에 의존하지 않는다"를 선언하고
        //          check 하세요 (힌트: resideInAnyPackage가 여러 패키지를 받는다)
    }

    @Test
    @DisplayName("policy 클래스는 *Validator로 끝나고, Spring을 모른다 (Step 1이 가능한 이유)")
    void policy는_순수_Validator() {
        // TODO 2 : classes()로 "..policy.. 클래스는 SimpleName이 Validator로 끝난다"를,
        // TODO 3 : noClasses()로 "..policy.. 클래스는 org.springframework..에 의존하지
        //          않는다"를 각각 선언하고 check 하세요
        //          (이 규칙이 있는 한, 정책 검증은 영원히 new로 테스트할 수 있다!)
    }
}
