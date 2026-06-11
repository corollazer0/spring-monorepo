package com.batchflow.step06.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Batch Step 6 — example B] 도메인 시드 데이터 — 이후 모든 Step의 공통 출발선
 *
 * Step 7부터의 Reader들은 이 데이터를 읽는다. 출발선이 흔들리면 모든 검증이 흔들리므로
 * 시드 자체를 테스트로 봉인한다 (TestCraft Step 3의 교훈 — 시드 검증이 인코딩 버그를 잡았다).
 *
 * 핵심 시나리오 수치:
 * - 회원 50명 = ACTIVE 30(최근 20 + 휴면대상 10) + DORMANT 15 + WITHDRAWN 5
 * - 휴면 전환 대상: ACTIVE이면서 2025-06-11(기준일 1년 전) 이전 로그인 → 정확히 10명
 * - 2026-06-10자 거래 9건 (정산 캡스톤 시나리오)
 */
@SpringBootTest
@DisplayName("도메인 시드 데이터 검증")
class DomainSeedTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("회원 분포: 전체 50 = ACTIVE 30 + DORMANT 15 + WITHDRAWN 5")
    void seed_회원분포_고정시나리오() {
        assertThat(count("SELECT COUNT(*) FROM member")).isEqualTo(50);
        assertThat(count("SELECT COUNT(*) FROM member WHERE status = 'ACTIVE'")).isEqualTo(30);
        assertThat(count("SELECT COUNT(*) FROM member WHERE status = 'DORMANT'")).isEqualTo(15);
        assertThat(count("SELECT COUNT(*) FROM member WHERE status = 'WITHDRAWN'")).isEqualTo(5);
    }

    @Test
    @DisplayName("휴면 전환 대상(ACTIVE + 1년 이상 미접속)은 정확히 10명 — Step 7~10의 기준값")
    void seed_휴면전환대상_10명() {
        Integer dormantTargets = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM member " +
                        "WHERE status = 'ACTIVE' AND last_login_at < '2025-06-11'",
                Integer.class);
        assertThat(dormantTargets).isEqualTo(10);
    }

    @Test
    @DisplayName("2026-06-10자 거래는 9건, 정산 대상 회원은 5명 — Step 13 캡스톤의 기준값")
    void seed_정산시나리오_고정수치() {
        assertThat(count(
                "SELECT COUNT(*) FROM bank_transaction WHERE transaction_date = '2026-06-10'"))
                .isEqualTo(9);
        assertThat(count(
                "SELECT COUNT(DISTINCT member_id) FROM bank_transaction WHERE transaction_date = '2026-06-10'"))
                .isEqualTo(5);
    }

    private Integer count(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
}
