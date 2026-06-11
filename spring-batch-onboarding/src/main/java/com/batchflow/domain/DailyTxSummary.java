package com.batchflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 일일 거래 집계 행 — 정산 리더(GROUP BY 쿼리)의 결과 1행.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyTxSummary {

    private Long memberId;
    private long totalDeposit;
    private long totalWithdraw;
    private int txCount;
}
