package com.batchflow.step13.answer;

import com.batchflow.domain.DailyTxSummary;
import com.batchflow.domain.Settlement;
import com.batchflow.processor.SettlementProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 13 — answer] 정산 계산 규칙의 순수 단위 테스트
 *
 * 전략: "순액 = 입금 - 출금"이라는 계산 규칙과 그 경계(음수/0)는
 * DB 없이 ms 단위로 검증한다 — 케이스는 여기에 쌓는다 (Step 9의 철학).
 */
@DisplayName("정산 프로세서 (캡스톤 모범답안)")
class SettlementProcessorAnswerTest {

    private static final LocalDate TARGET = LocalDate.of(2026, 6, 10);

    private final SettlementProcessor processor = new SettlementProcessor(TARGET);

    @Test
    @DisplayName("순액 = 입금 - 출금, 모든 필드가 정확히 옮겨진다")
    void process_일반집계_정산변환() {
        // given : m1 시나리오
        DailyTxSummary summary = DailyTxSummary.builder()
                .memberId(1L).totalDeposit(30000).totalWithdraw(5000).txCount(3)
                .build();

        // when
        Settlement settlement = processor.process(summary);

        // then : 계산값 + 이관 필드 + 주입된 날짜까지
        assertThat(settlement.getNetAmount()).isEqualTo(25000);
        assertThat(settlement.getMemberId()).isEqualTo(1L);
        assertThat(settlement.getTotalDeposit()).isEqualTo(30000);
        assertThat(settlement.getTotalWithdraw()).isEqualTo(5000);
        assertThat(settlement.getTransactionCount()).isEqualTo(3);
        assertThat(settlement.getSettlementDate()).isEqualTo(TARGET);
    }

    @Test
    @DisplayName("출금이 더 크면 순액은 음수다 (절대값/0 보정 같은 거 하지 마라!)")
    void process_출금초과_음수순액() {
        // given : m3 시나리오
        DailyTxSummary summary = DailyTxSummary.builder()
                .memberId(3L).totalDeposit(0).totalWithdraw(10000).txCount(2)
                .build();

        // when & then : 음수가 그대로 보존되는 것이 "정확한 정산"이다
        assertThat(processor.process(summary).getNetAmount()).isEqualTo(-10000);
    }

    @Test
    @DisplayName("입금 = 출금이면 순액 0 (경계값)")
    void process_입출금동일_순액0() {
        // given : m4 시나리오
        DailyTxSummary summary = DailyTxSummary.builder()
                .memberId(4L).totalDeposit(15000).totalWithdraw(15000).txCount(2)
                .build();

        // when & then
        assertThat(processor.process(summary).getNetAmount()).isZero();
    }
}
