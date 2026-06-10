package com.testonboarding.support.security;

import com.testonboarding.member.domain.Role;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * [심화 Step 11] 우리 도메인 전용 인증 어노테이션.
 *
 * @WithMockUser의 한계: principal이 Security의 기본 User라서
 * 우리 도메인 정보(memberId, nickname)가 없다. LoginMember를 쓰는 코드를
 * 테스트하려면 도메인을 아는 인증이 필요하다.
 *
 * 사용:
 *   @WithMockMember                                      // 기본: writer1/USER
 *   @WithMockMember(username = "admin", role = Role.ADMIN)
 *
 * 동작 원리: @WithSecurityContext가 지정한 팩토리가 테스트 실행 전에
 * SecurityContext를 만들어 심는다 — @WithMockUser도 내부적으로 같은 구조다.
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockMemberSecurityContextFactory.class)
public @interface WithMockMember {

    String username() default "writer1";

    String nickname() default "글쓴이일호";

    long memberId() default 1L;

    Role role() default Role.USER;
}
