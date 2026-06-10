package com.testonboarding.step02.exercise;

import com.testonboarding.member.dao.MemberDao;
import com.testonboarding.member.service.MemberService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * [Step 2 — exercise] MemberService.signup 테스트를 직접 작성해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (example의 BoardServiceTest를 참고)
 * 3. .\gradlew :spring-test-onboarding:test 로 통과를 확인한다
 *
 * 검증 대상 비즈니스 규칙 (MemberService.signup):
 * - 중복 아이디면 DuplicateUsernameException + insert가 호출되면 안 됨
 * - 정상 가입이면 비밀번호가 "인코딩된 값"으로 저장되고 권한은 USER
 *
 * 힌트: 협력 객체가 2개다 — MemberDao와 PasswordEncoder 둘 다 Mock이 필요하다.
 *       (PasswordPolicyValidator는 순수 자바라 진짜를 그대로 쓴다 → 비밀번호는 정책을 만족하는 값으로!)
 */
@Disabled("과제: docs/test/education/FOR-Test-Step02.md 참고 후 @Disabled를 제거하고 완성하세요")
@ExtendWith(MockitoExtension.class)
@DisplayName("회원 서비스 (연습문제)")
class MemberServiceExerciseTest {

    @Mock
    private MemberDao memberDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("정상 가입이면 인코딩된 비밀번호와 USER 권한으로 저장된다")
    void signup_정상가입_인코딩비밀번호와USER권한으로저장() {
        // given :
        // TODO 1: "아직 없는 아이디"라는 시나리오를 만드세요
        //         → memberDao.findByUsername(...)이 null을 돌려주도록 stubbing
        // TODO 2: passwordEncoder.encode("spring123!")가 "ENCODED"를 돌려주도록 stubbing
        // TODO 3: 가입 요청 DTO를 만드세요 — new MemberSignupRequest("newbie", "spring123!", "새내기")

        // when :
        // TODO 4: memberService.signup(request) 호출

        // then :
        // TODO 5: ArgumentCaptor<Member>로 memberDao.insert에 전달된 Member를 캡처하세요
        // TODO 6: 캡처한 Member의 password가 "ENCODED"인지 검증하세요 (평문 저장 버그 방지!)
        // TODO 7: 캡처한 Member의 role이 Role.USER인지 검증하세요
    }

    @Test
    @DisplayName("중복 아이디면 예외가 발생하고 저장은 시도되지 않는다")
    void signup_중복아이디_예외발생및저장안함() {
        // given :
        // TODO 8: "이미 존재하는 아이디"라는 시나리오를 만드세요
        //         → findByUsername이 아무 Member나 (Member.builder()...build()) 돌려주도록 stubbing

        // when & then :
        // TODO 9: assertThatThrownBy로 DuplicateUsernameException 발생을 검증하세요
        // TODO 10: memberDao.insert가 "절대 호출되지 않았음"을 검증하세요
        //          (then(memberDao).should(never()).insert(...) 또는 verify(memberDao, never())...)
    }
}
