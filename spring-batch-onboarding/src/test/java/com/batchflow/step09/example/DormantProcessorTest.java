package com.batchflow.step09.example;

import com.batchflow.domain.Member;
import com.batchflow.processor.ActiveOnlyValidationProcessor;
import com.batchflow.processor.DormantConvertProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.support.CompositeItemProcessor;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 9 — example] Processor는 순수 자바다 — Spring 없이 단위 테스트!
 *
 * 깨달음의 순간: ItemProcessor의 process()는 그냥 메서드다.
 * Job도, 컨텍스트도, DB도 필요 없다 — new 해서 부르면 끝 (ms 단위, TestCraft의 세계).
 *
 * 비즈니스 규칙(검증/변환)의 케이스는 전부 여기서 빠르게 쌓고,
 * Job 통합 테스트(Step 10)는 "조립이 맞는지"만 본다 — 테스트 피라미드의 배치 적용.
 */
@DisplayName("휴면 전환 프로세서들 (순수 단위)")
class DormantProcessorTest {

    private static final LocalDateTime FIXED_DORMANT_AT =
            LocalDateTime.of(2026, 6, 11, 3, 0, 0); // 고정 시각 — 결정적 테스트의 전제

    @Nested
    @DisplayName("검증 프로세서 — 심층 방어")
    class Validation {

        private final ActiveOnlyValidationProcessor processor = new ActiveOnlyValidationProcessor();

        @Test
        @DisplayName("ACTIVE 회원은 그대로 통과한다")
        void process_ACTIVE회원_통과() {
            // given
            Member active = Member.builder().memberId(21L).status(Member.STATUS_ACTIVE).build();

            // when & then
            assertThat(processor.process(active)).isSameAs(active);
        }

        @Test
        @DisplayName("이미 DORMANT면 null(필터) — 이중 전환 사고 방지")
        void process_DORMANT회원_필터링() {
            // given : 리더 SQL이 잘못 바뀌어 DORMANT가 흘러들어온 상황
            Member alreadyDormant = Member.builder()
                    .memberId(31L).status(Member.STATUS_DORMANT).build();

            // when & then : null = 조용히 버림 (FILTER_COUNT) — 예외가 아니다
            assertThat(processor.process(alreadyDormant)).isNull();
        }
    }

    @Nested
    @DisplayName("변환 프로세서 — 상태와 시각")
    class Convert {

        private final DormantConvertProcessor processor =
                new DormantConvertProcessor(FIXED_DORMANT_AT);

        @Test
        @DisplayName("상태를 DORMANT로 바꾸고 주입받은 전환 시각을 찍는다")
        void process_ACTIVE후보_DORMANT변환() {
            // given
            Member candidate = Member.builder()
                    .memberId(21L).name("회원21").email("member21@test.com")
                    .status(Member.STATUS_ACTIVE)
                    .build();

            // when
            Member converted = processor.process(candidate);

            // then : 바뀌어야 할 것(상태/시각)과 보존되어야 할 것(이름/이메일) 모두 검증
            assertThat(converted.getStatus()).isEqualTo(Member.STATUS_DORMANT);
            assertThat(converted.getDormantAt()).isEqualTo(FIXED_DORMANT_AT);
            assertThat(converted.getName()).isEqualTo("회원21");
            assertThat(converted.getEmail()).isEqualTo("member21@test.com");
        }
    }

    @Nested
    @DisplayName("Composite — 검증 → 변환 체인")
    class Composite {

        /**
         * CompositeItemProcessor: 프로세서들을 줄줄이 잇는다.
         * 체인 중간에 null이 나오면 거기서 멈춘다 — 뒤 프로세서는 호출되지 않는다.
         */
        private CompositeItemProcessor<Member, Member> compositeOf() throws Exception {
            CompositeItemProcessor<Member, Member> composite = new CompositeItemProcessor<>();
            composite.setDelegates(Arrays.asList(
                    new ActiveOnlyValidationProcessor(),
                    new DormantConvertProcessor(FIXED_DORMANT_AT)));
            composite.afterPropertiesSet(); // 수동 조립 시 초기화 검증 호출 (잊기 쉬운 한 줄!)
            return composite;
        }

        @Test
        @DisplayName("ACTIVE 후보는 체인을 통과해 DORMANT로 변환된다")
        void composite_ACTIVE후보_검증후변환() throws Exception {
            // given
            Member candidate = Member.builder().memberId(21L).status(Member.STATUS_ACTIVE).build();

            // when
            Member result = compositeOf().process(candidate);

            // then
            assertThat(result.getStatus()).isEqualTo(Member.STATUS_DORMANT);
        }

        @Test
        @DisplayName("DORMANT는 검증 단계에서 끊겨 변환까지 가지 않는다")
        void composite_DORMANT_체인중단() throws Exception {
            // given
            Member alreadyDormant = Member.builder()
                    .memberId(31L).status(Member.STATUS_DORMANT)
                    .dormantAt(LocalDateTime.of(2024, 6, 1, 0, 0))
                    .build();

            // when
            Member result = compositeOf().process(alreadyDormant);

            // then : null로 끊겼고, 기존 dormantAt도 덮어쓰이지 않았다 (체인 중단의 증거)
            assertThat(result).isNull();
            assertThat(alreadyDormant.getDormantAt())
                    .isEqualTo(LocalDateTime.of(2024, 6, 1, 0, 0));
        }
    }
}
