package com.testonboarding.advanced.step10.example;

import com.testonboarding.auth.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [심화 Step 10 — example A] JWT 토큰 생성/검증기의 순수 단위 테스트
 *
 * JwtTokenProvider는 생성자로 secret과 유효기간을 받는 순수 클래스다.
 * 덕분에 테스트가 다음을 자유롭게 만들 수 있다:
 * - 만료된 토큰: 유효기간을 음수로 준 provider로 생성
 * - 위조 토큰: 다른 secret으로 서명한 provider로 생성
 *
 * "설정 주입이 테스트 가능성을 만든다" — 만약 secret이 클래스 안에 하드코딩되어
 * 있었다면 위조/만료 시나리오를 만들기 위해 곡예를 해야 했을 것이다.
 */
@DisplayName("JWT 토큰 프로바이더 (단위 테스트)")
class JwtTokenProviderTest {

    private static final String SECRET = "testcraft-jwt-secret-key-for-learning-only-do-not-use";
    private static final long ONE_HOUR = 3_600_000L;

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(SECRET, ONE_HOUR);
    }

    @Test
    @DisplayName("생성한 토큰에서 username과 role이 그대로 복원된다 (왕복 검증)")
    void createToken_정상생성_클레임복원() {
        // when
        String token = provider.createToken("writer1", "USER");

        // then : 직렬화-역직렬화 왕복 — Step 3의 insert 후 재조회와 같은 철학
        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getUsername(token)).isEqualTo("writer1");
        assertThat(provider.getRole(token)).isEqualTo("USER");
    }

    @Test
    @DisplayName("만료된 토큰은 검증에 실패한다")
    void validateToken_만료된토큰_false() {
        // given : 유효기간이 음수인 provider → 태어날 때부터 만료된 토큰
        JwtTokenProvider expiredProvider = new JwtTokenProvider(SECRET, -1000L);
        String expiredToken = expiredProvider.createToken("writer1", "USER");

        // when & then : 같은 키로 검증해도 만료 검사에서 걸린다
        assertThat(provider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("다른 키로 서명된(위조) 토큰은 검증에 실패한다")
    void validateToken_위조서명_false() {
        // given : 공격자가 자기 키로 서명한 토큰 (내용은 그럴듯하다!)
        JwtTokenProvider attackerProvider = new JwtTokenProvider(
                "attacker-secret-key-that-is-long-enough-32bytes!", ONE_HOUR);
        String forgedToken = attackerProvider.createToken("admin", "ADMIN");

        // when & then : 서명이 우리 키와 안 맞으므로 거부 — JWT 보안의 핵심
        assertThat(provider.validateToken(forgedToken)).isFalse();
    }

    @ParameterizedTest(name = "[{index}] \"{0}\"")
    @ValueSource(strings = {"garbage", "a.b.c", ""})
    @DisplayName("형식이 깨진 문자열도 예외 없이 false")
    void validateToken_깨진형식_false(String brokenToken) {
        // 검증기가 쓰레기 입력에 예외를 던지면 필터에서 500이 터진다 — false가 계약이다
        assertThat(provider.validateToken(brokenToken)).isFalse();
    }
}
