package com.batchflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 일일 정산 결과 — settlement 테이블 1행.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

    private Long settlementId;
    private LocalDate settlementDate;
    private Long memberId;
    private long totalDeposit;
    private long totalWithdraw;
    private long netAmount;
    private int transactionCount;
}
