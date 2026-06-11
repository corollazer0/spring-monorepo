package com.batchflow.step09.answer;

import com.batchflow.domain.Member;
import com.batchflow.processor.EmailMaskingProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 9 — answer] EmailMaskingExerciseTest 모범답안
 *
 * 채점 포인트:
 * - Spring 없이 new + process() 직접 호출로 검증했는가
 * - 경계값(2자)과 불량 입력(필터)까지 다뤘는가 — 정상 케이스만 보면 반쪽 테스트
 */
@DisplayName("이메일 마스킹 프로세서 (모범답안)")
class EmailMaskingAnswerTest {

    private final EmailMaskingProcessor processor = new EmailMaskingProcessor();

    @Test
    @DisplayName("일반 이메일은 앞 2자만 남기고 마스킹된다")
    void process_일반이메일_마스킹() {
        // given (TODO 1 답)
        Member member = Member.builder()
                .memberId(21L)
                .email("member21@test.com")
                .status(Member.STATUS_ACTIVE)
                .build();

        // when (TODO 2 답)
        Member result = processor.process(member);

        // then (TODO 3 답)
        assertThat(result.getEmail()).isEqualTo("me***@test.com");
    }

    @Test
    @DisplayName("로컬파트가 2자면 전체 마스킹된다 (경계값!)")
    void process_로컬파트2자_전체마스킹() {
        // given & when & then (TODO 4 답)
        Member member = Member.builder().memberId(1L).email("ab@test.com").build();

        assertThat(processor.process(member).getEmail()).isEqualTo("**@test.com");
    }

    @Test
    @DisplayName("@가 없는 불량 이메일은 필터링된다")
    void process_형식불량_null반환() {
        // given & when & then (TODO 5 답)
        Member member = Member.builder().memberId(1L).email("broken-email").build();

        assertThat(processor.process(member)).isNull();
    }
}
