package com.batchflow.processor;

import com.batchflow.domain.DailyTxSummary;
import com.batchflow.domain.Settlement;
import org.springframework.batch.item.ItemProcessor;

import java.time.LocalDate;

/**
 * Step 13 캡스톤: 정산 계산 프로세서 — 집계 행을 정산 결과로 변환
 *
 * 계산 규칙: 순액(netAmount) = 총입금 - 총출금 (음수 가능!)
 * 순수 자바 — 계산 규칙의 케이스는 단위 테스트로 쌓는다 (Step 9의 철학).
 */
public class SettlementProcessor implements ItemProcessor<DailyTxSummary, Settlement> {

    private final LocalDate settlementDate;

    public SettlementProcessor(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    @Override
    public Settlement process(DailyTxSummary summary) {
        return Settlement.builder()
                .settlementDate(settlementDate)
                .memberId(summary.getMemberId())
                .totalDeposit(summary.getTotalDeposit())
                .totalWithdraw(summary.getTotalWithdraw())
                .netAmount(summary.getTotalDeposit() - summary.getTotalWithdraw())
                .transactionCount(summary.getTxCount())
                .build();
    }
}
