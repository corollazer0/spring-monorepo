package com.testonboarding.advanced.step14.exercise;

import com.testonboarding.board.controller.BoardController;
import com.testonboarding.board.service.BoardService;
import com.testonboarding.config.SecurityConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * [심화 Step 14 — exercise] 게시글 단건 조회 API를 문서화해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (힌트: example의 MemberApiDocsTest + 아래 ⚠️)
 * 3. .\gradlew :spring-test-onboarding:test 후
 *    build/generated-snippets/post-get/ 폴더가 생겼는지 눈으로 확인한다
 *
 * ⚠️ 경로 변수({postId})를 pathParameters로 문서화하려면 요청 빌더를
 * MockMvcRequestBuilders가 아니라 **RestDocumentationRequestBuilders**의 get으로
 * 써야 한다 — URL 템플릿 정보를 보존하는 쪽은 후자다 (이 Step의 1번 함정!)
 */
@Disabled("과제: docs/test/education/FOR-Test-Step14.md 참고 후 @Disabled를 제거하고 완성하세요")
@WebMvcTest(BoardController.class)
@Import(SecurityConfig.class)
@AutoConfigureRestDocs
@DisplayName("게시글 API 문서화 (연습문제)")
class PostApiDocsExerciseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoardService boardService;

    @Test
    @DisplayName("단건 조회 문서화: 경로 변수 + 응답 필드 5종")
    void getPost_문서화() throws Exception {
        // given : ① boardService.getPost(1L)가 PostResponse를 반환하도록 스텁하세요
        //          (PostResponse.builder() — postId/writer/title/content/createdAt 전부 채울 것:
        //           null 필드는 JSON에서 빠질 수 있어 문서화가 어긋난다!)
        // TODO 1

        // when & then : ② RestDocumentationRequestBuilders.get("/api/posts/{postId}", 1L)로
        //               요청하고 200을 확인한 뒤, document("post-get", ...)로
        //               - pathParameters: postId
        //               - responseFields: postId, writer, title, content, createdAt
        //               를 문서화하세요
        // TODO 2

        // then : ③ build/generated-snippets/post-get/path-parameters.adoc 파일이
        //          존재하는지 검증하세요 (Files.exists)
        // TODO 3
    }
}
