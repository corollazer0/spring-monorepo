package com.testonboarding.advanced.step11.answer;

import com.testonboarding.board.dao.BoardDao;
import com.testonboarding.comment.dao.CommentDao;
import com.testonboarding.comment.service.CommentService;
import com.testonboarding.common.exception.NotCommentOwnerException;
import com.testonboarding.support.CommentFixture;
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
 * [심화 Step 11 — answer] CommentFixtureExerciseTest 모범답안
 *
 * support/CommentFixture.java도 함께 보세요 (PostFixture와 같은 구조).
 *
 * 채점 포인트:
 * - Fixture 교체 후 "이 시나리오의 핵심"(writer)만 코드에 남았는가
 * - 기본값은 Fixture에, 시나리오 값은 테스트에 — 책임이 나뉘었는가
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("댓글 Fixture 적용 (모범답안)")
class CommentFixtureAnswerTest {

    @Mock
    private CommentDao commentDao;

    @Mock
    private BoardDao boardDao;

    @InjectMocks
    private CommentService commentService;

    @Test
    @DisplayName("타인 댓글 삭제 → 예외")
    void deleteComment_타인댓글_예외() {
        // given (TODO 1 답) : 핵심인 "writer2"만 보인다
        given(commentDao.findById(1L)).willReturn(CommentFixture.comment(1L, "writer2"));

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(1L, "hacker"))
                .isInstanceOf(NotCommentOwnerException.class);
    }

    @Test
    @DisplayName("본인 댓글 삭제 → 성공")
    void deleteComment_본인댓글_삭제성공() {
        // given (TODO 2 답)
        given(commentDao.findById(1L)).willReturn(CommentFixture.comment(1L, "writer1"));

        // when
        commentService.deleteComment(1L, "writer1");

        // then
        verify(commentDao).deleteById(1L);
    }
}
