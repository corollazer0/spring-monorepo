package com.batchflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대량 성능 실습용 레코드 (심화 Step 19) — bulk_member/bulk_archive 대응.
 * 일부러 가볍다: 성능 실습의 변인은 데이터 모양이 아니라 처리 구조니까.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkRecord {

    private Long memberId;
    private String name;
}
