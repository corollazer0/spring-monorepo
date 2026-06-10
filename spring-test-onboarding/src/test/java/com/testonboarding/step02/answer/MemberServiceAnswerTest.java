package com.testonboarding.step02.answer;

import com.testonboarding.common.exception.DuplicateUsernameException;
import com.testonboarding.member.dao.MemberDao;
import com.testonboarding.member.domain.Member;
import com.testonboarding.member.domain.Role;
import com.testonboarding.member.dto.MemberSignupRequest;
import com.testonboarding.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * [Step 2 — answer] MemberServiceExerciseTest 모범답안
 *
 * 채점 포인트:
 * - 협력 객체 2개(MemberDao, PasswordEncoder)를 모두 Mock으로 처리했는가
 * - "인코딩된 비밀번호로 저장"을 ArgumentCaptor로 증명했는가 (반환값만으론 알 수 없다)
 * - 중복 시 "insert가 호출되지 않음"까지 검증했는가
 * - 비밀번호를 정책(8자+숫자+특수문자)을 만족하는 값으로 줬는가
 *   (PasswordPolicyValidator는 Mock이 아닌 진짜이므로 정책 위반이면 다른 예외가 터진다!)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("회원 서비스 (모범답안)")
class MemberServiceAnswerTest {

    @Mock
    private MemberDao memberDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("정상 가입이면 인코딩된 비밀번호와 USER 권한으로 저장된다")
    void signup_정상가입_인코딩비밀번호와USER권한으로저장() {
        // given (TODO 1~3 답)
        given(memberDao.findByUsername("newbie")).willReturn(null);
        given(passwordEncoder.encode("spring123!")).willReturn("ENCODED");
        MemberSignupRequest request = new MemberSignupRequest("newbie", "spring123!", "새내기");

        // when (TODO 4 답)
        memberService.signup(request);

        // then (TODO 5~7 답)
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberDao).insert(captor.capture());

        Member savedMember = captor.getValue();
        assertThat(savedMember.getPassword()).isEqualTo("ENCODED"); // 평문 "spring123!"이면 실패!
        assertThat(savedMember.getRole()).isEqualTo(Role.USER);
        assertThat(savedMember.getUsername()).isEqualTo("newbie");
    }

    @Test
    @DisplayName("중복 아이디면 예외가 발생하고 저장은 시도되지 않는다")
    void signup_중복아이디_예외발생및저장안함() {
        // given (TODO 8 답) : 이미 같은 아이디의 회원이 존재한다
        given(memberDao.findByUsername("newbie"))
                .willReturn(Member.builder().username("newbie").build());

        MemberSignupRequest request = new MemberSignupRequest("newbie", "spring123!", "새내기");

        // when & then (TODO 9 답)
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(DuplicateUsernameException.class)
                .hasMessageContaining("newbie");

        // (TODO 10 답) 중복인데 insert까지 가버리는 버그를 잡는 검증
        then(memberDao).should(never()).insert(any(Member.class));
    }
}
