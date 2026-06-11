package com.batchflow.step09.exercise;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * [Batch Step 9 — exercise] EmailMaskingProcessor를 순수 단위 테스트해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example의 DormantProcessorTest를 참고 — Spring 불필요!)
 * 3. .\gradlew :spring-batch-onboarding:test 로 통과를 확인한다
 *
 * 마스킹 규칙 (EmailMaskingProcessor Javadoc):
 * - "abcdef@test.com" → "ab***@test.com"
 * - 로컬파트 2자 이하 → "**@도메인"
 * - null 또는 @ 없음 → null 반환(필터)
 */
@Disabled("과제: docs/batch/education/FOR-BatchFlow-Step09.md 참고 후 @Disabled를 제거하고 완성하세요")
@DisplayName("이메일 마스킹 프로세서 (연습문제)")
class EmailMaskingExerciseTest {

    @Test
    @DisplayName("일반 이메일은 앞 2자만 남기고 마스킹된다")
    void process_일반이메일_마스킹() {
        // given : EmailMaskingProcessor를 new로 만들고,
        //         email이 "member21@test.com"인 Member를 만드세요
        // TODO 1

        // when : process()를 직접 호출하세요
        // TODO 2

        // then : 이메일이 "me***@test.com"인지 검증하세요
        // TODO 3
    }

    @Test
    @DisplayName("로컬파트가 2자면 전체 마스킹된다 (경계값!)")
    void process_로컬파트2자_전체마스킹() {
        // given & when & then : "ab@test.com" → "**@test.com" 을 검증하세요
        // TODO 4
    }

    @Test
    @DisplayName("@가 없는 불량 이메일은 필터링된다")
    void process_형식불량_null반환() {
        // given & when & then : email "broken-email"이면 process 결과가 null인지 검증하세요
        // TODO 5
    }
}
