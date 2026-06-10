package com.testonboarding.advanced.step11.exercise;

import com.testonboarding.board.dao.BoardDao;
import com.testonboarding.comment.dao.CommentDao;
import com.testonboarding.comment.domain.Comment;
import com.testonboarding.comment.service.CommentService;
import com.testonboarding.common.exception.NotCommentOwnerException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * [심화 Step 11 — exercise] CommentFixture를 직접 만들어 적용해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. support 패키지에 CommentFixture를 만든다 (PostFixture를 본떠서):
 *      - aComment(): commentId=1L, postId=1L, writer="writer1", content="기본 댓글" 기본값 builder
 *      - comment(Long commentId, String writer): 자주 쓰는 형태
 * 3. 아래 테스트들의 "장황한 builder"를 Fixture 호출로 교체한다 (TODO)
 * 4. .\gradlew :spring-test-onboarding:test 로 통과를 확인한다
 *
 * 비교 포인트: 교체 후 각 테스트에서 "시나리오의 핵심 값"만 남았는가?
 */
@Disabled("과제: docs/test/education/FOR-Test-Step11.md 참고 후 @Disabled를 제거하고 완성하세요")
@ExtendWith(MockitoExtension.class)
@DisplayName("댓글 Fixture 적용 (연습문제)")
class CommentFixtureExerciseTest {

    @Mock
    private CommentDao commentDao;

    @Mock
    private BoardDao boardDao;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("타인 댓글 삭제 → 예외")
    void deleteComment_타인댓글_예외() {
        // given :
        // TODO 1: 아래의 장황한 builder를 CommentFixture.comment(1L, "writer2") 호출로 교체하세요
        given(commentDao.findById(1L)).willReturn(
                Comment.builder()
                        .commentId(1L)
                        .postId(1L)
                        .writer("writer2")
                        .content("아무 내용")
                        .build());

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(1L, "hacker"))
                .isInstanceOf(NotCommentOwnerException.class);
    }

    @Test
    @DisplayName("본인 댓글 삭제 → 성공")
    void deleteComment_본인댓글_삭제성공() {
        // given :
        // TODO 2: 이 builder도 Fixture로 교체하세요
        given(commentDao.findById(1L)).willReturn(
                Comment.builder()
                        .commentId(1L)
                        .postId(1L)
                        .writer("writer1")
                        .content("아무 내용")
                        .build());

        // when
        commentService.deleteComment(1L, "writer1");

        // then
        verify(commentDao).deleteById(1L);
    }
}
