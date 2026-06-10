package com.testonboarding.step09.answer;

import com.testonboarding.board.dao.BoardDao;
import com.testonboarding.board.domain.Post;
import com.testonboarding.comment.dao.CommentDao;
import com.testonboarding.comment.domain.Comment;
import com.testonboarding.comment.dto.CommentCreateRequest;
import com.testonboarding.comment.service.CommentService;
import com.testonboarding.common.exception.CommentNotFoundException;
import com.testonboarding.common.exception.NotCommentOwnerException;
import com.testonboarding.common.exception.PostNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * [Step 9 — answer] 댓글 Service 단위 테스트 (Mockito)
 *
 * 전략: 비즈니스 규칙 3가지(없는 글 404 / 본인만 삭제 / 없는 댓글 404)는
 * DB 없이 빠르게 검증할 수 있는 "판단 로직"이므로 Mockito 단위가 적합.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("댓글 서비스 (캡스톤 모범답안)")
class CommentServiceAnswerTest {

    @Mock
    private CommentDao commentDao;

    @Mock
    private BoardDao boardDao;

    @InjectMocks
    private CommentService commentService;

    @Nested
    @DisplayName("댓글 작성")
    class CreateComment {

        @Test
        @DisplayName("없는 게시글이면 예외가 발생하고 insert는 호출되지 않는다")
        void createComment_없는게시글_예외및저장안함() {
            // given
            given(boardDao.findById(99L)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> commentService.createComment(
                    99L, "writer1", new CommentCreateRequest("댓글")))
                    .isInstanceOf(PostNotFoundException.class)
                    .hasMessageContaining("99");

            then(commentDao).should(never()).insert(any(Comment.class));
        }

        @Test
        @DisplayName("정상 작성이면 postId/writer가 채워진 Comment가 DAO에 전달된다")
        void createComment_정상_댓글내용검증() {
            // given
            given(boardDao.findById(1L)).willReturn(
                    Post.builder().postId(1L).writer("writer2").title("t").content("c").build());
            CommentCreateRequest request = new CommentCreateRequest("좋은 글이네요");

            // when
            commentService.createComment(1L, "writer1", request);

            // then : Captor로 내용물 검증 — postId 누락/작성자 바꿔치기 버그 방지
            ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
            verify(commentDao).insert(captor.capture());

            Comment saved = captor.getValue();
            assertThat(saved.getPostId()).isEqualTo(1L);
            assertThat(saved.getWriter()).isEqualTo("writer1");
            assertThat(saved.getContent()).isEqualTo("좋은 글이네요");
        }
    }

    @Nested
    @DisplayName("댓글 삭제")
    class DeleteComment {

        @Test
        @DisplayName("없는 댓글이면 CommentNotFoundException")
        void deleteComment_없는댓글_예외발생() {
            // given
            given(commentDao.findById(99L)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> commentService.deleteComment(99L, "writer1"))
                    .isInstanceOf(CommentNotFoundException.class);
        }

        @Test
        @DisplayName("작성자가 아니면 예외가 발생하고 delete는 호출되지 않는다")
        void deleteComment_타인댓글_예외및삭제안함() {
            // given
            given(commentDao.findById(1L)).willReturn(
                    Comment.builder().commentId(1L).postId(1L).writer("writer2").content("c").build());

            // when & then
            assertThatThrownBy(() -> commentService.deleteComment(1L, "hacker"))
                    .isInstanceOf(NotCommentOwnerException.class)
                    .hasMessageContaining("hacker");

            then(commentDao).should(never()).deleteById(any(Long.class));
        }

        @Test
        @DisplayName("본인 댓글이면 삭제가 호출된다")
        void deleteComment_본인댓글_삭제성공() {
            // given
            given(commentDao.findById(1L)).willReturn(
                    Comment.builder().commentId(1L).postId(1L).writer("writer1").content("c").build());

            // when
            commentService.deleteComment(1L, "writer1");

            // then
            verify(commentDao).deleteById(1L);
        }
    }
}
