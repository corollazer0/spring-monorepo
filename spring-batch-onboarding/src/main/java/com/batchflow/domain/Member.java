package com.batchflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 회원 도메인 — 순수 자바 POJO (JPA 엔티티 아님!).
 *
 * JDBC RowMapper로 매핑하므로 기본 생성자 + setter가 필요하다.
 * status는 ACTIVE / DORMANT / WITHDRAWN.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_DORMANT = "DORMANT";
    public static final String STATUS_WITHDRAWN = "WITHDRAWN";

    private Long memberId;
    private String name;
    private String email;
    private String status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime dormantAt;
}
