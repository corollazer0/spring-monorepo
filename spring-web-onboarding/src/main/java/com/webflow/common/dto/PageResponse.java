package com.webflow.common.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 페이징 응답 규약 — 모든 목록 API는 이 형태로 응답한다.
 *
 * 화면이 페이지 내비게이션을 그리는 데 필요한 4종 세트:
 * content(이번 페이지 데이터) + page/size(현재 위치) + totalCount/totalPages(전체 규모)
 */
@Getter
@Builder
public class PageResponse<T> {

    private final List<T> content;
    private final int page;        // 1부터 시작
    private final int size;
    private final long totalCount;
    private final int totalPages;

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalCount) {
        return PageResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalCount(totalCount)
                .totalPages(calculateTotalPages(totalCount, size))
                .build();
    }

    /**
     * 전체 페이지 = ceil(전체 건수 / 페이지 크기). 0건이면 0페이지.
     * (11건/size 5 = 3페이지 — 올림 계산의 경계가 단골 버그!)
     */
    private static int calculateTotalPages(long totalCount, int size) {
        if (totalCount == 0) {
            return 0;
        }
        return (int) ((totalCount + size - 1) / size);
    }
}
