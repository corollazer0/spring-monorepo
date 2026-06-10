package com.testonboarding.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

/**
 * JWT 토큰 생성/검증기 — 심화 Step 10.
 *
 * 토큰 구성: subject=username, claim "role"=권한, 만료시각 포함, HS256 서명.
 * 서명 키는 32바이트 이상이어야 한다 (HS256 요구사항).
 * Bean 등록은 SecurityConfig의 @Bean 메서드가 담당한다 (설정값 주입 포함).
 *
 * 테스트 포인트: 생성자에 secret/유효기간을 주입받는 순수 클래스이므로,
 * 테스트에서 "만료된 토큰"(유효기간 음수)이나 "다른 키로 서명된 위조 토큰"을
 * 자유롭게 만들어낼 수 있다 — 설정 주입이 테스트 가능성을 만든다.
 */
public class JwtTokenProvider {

    private final Key key;
    private final long validityMillis;

    public JwtTokenProvider(String secret, long validityMillis) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.validityMillis = validityMillis;
    }

    public String createToken(String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMillis);

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsername(String token) {
        return parse(token).getBody().getSubject();
    }

    public String getRole(String token) {
        return parse(token).getBody().get("role", String.class);
    }

    /**
     * 서명 위조, 만료, 형식 오류 — 모든 무효 사유에 대해 false.
     */
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token); // 서명 검증 + 만료 검사가 여기서 일어난다
    }
}
