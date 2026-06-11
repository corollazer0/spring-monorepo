package com.batchflow.domain;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * member 테이블 한 행 → Member 객체 (Step 7 커서/Step 8 페이징 리더 공용).
 */
public class MemberRowMapper implements RowMapper<Member> {

    @Override
    public Member mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp lastLogin = rs.getTimestamp("last_login_at");
        Timestamp dormantAt = rs.getTimestamp("dormant_at");
        return Member.builder()
                .memberId(rs.getLong("member_id"))
                .name(rs.getString("name"))
                .email(rs.getString("email"))
                .status(rs.getString("status"))
                .lastLoginAt(lastLogin == null ? null : lastLogin.toLocalDateTime())
                .dormantAt(dormantAt == null ? null : dormantAt.toLocalDateTime())
                .build();
    }
}
