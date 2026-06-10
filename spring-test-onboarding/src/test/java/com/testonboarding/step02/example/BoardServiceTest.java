package com.testonboarding.step02.example;

import com.testonboarding.board.dao.BoardDao;
import com.testonboarding.board.domain.Post;
import com.testonboarding.board.dto.PostCreateRequest;
import com.testonboarding.board.dto.PostResponse;
import com.testonboarding.board.dto.PostUpdateRequest;
import com.testonboarding.board.service.BoardService;
import com.testonboarding.common.exception.NotPostOwnerException;
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
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * [Step 2 — example] Service 비즈니스 로직 테스트 (Mockito)
 *
 * 문제 상황: BoardService는 BoardDao를 주입받는다.
 * 진짜 DAO는 DB가 있어야 동작한다 → Service 로직 하나 보자고 매번 DB를 띄울 수는 없다.
 *
 * 해결: Mockito가 BoardDao의 "가짜(Mock)"를 만들어준다.
 * - Mock은 우리가 시킨 대로만 행동한다: given(...).willReturn(...)  ← Stubbing
 * - Mock은 자기가 어떻게 호출됐는지 기억한다: verify(...)            ← 행위 검증
 *
 * 이 테스트가 보여주는 것:
 * 1. @ExtendWith(MockitoExtension.class) — Spring 없이 Mockito만 사용 (테스트 속도: ms 단위)
 * 2. @Mock + @InjectMocks — 가짜를 만들고 테스트 대상에 주입
 * 3. 상태 검증(반환값 assert)과 행위 검증(verify)의 구분과 사용처
 * 4. ArgumentCaptor — Mock에게 전달된 객체의 내용을 꺼내 검증
 * 5. "DAO를 호출하지 않았어야 한다"(never)도 중요한 검증이다
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("게시판 서비스 (Mockito 단위 테스트)")
class BoardServiceTest {

    /** 가짜 DAO — DB 없이도 우리가 시킨 대로 응답한다 */
    @Mock
    private BoardDao boardDao;

    /** 테스트 대상 — Mockito가 위의 Mock들을 생성자에 넣어 만들어준다 */
    @InjectMocks
    private BoardService boardService;

    @Nested
    @DisplayName("게시글 조회")
    class GetPost {

        @Test
        @DisplayName("존재하는 글이면 응답 DTO로 변환해 반환한다")
        void getPost_존재하는글_응답반환() {
            // given : "findById(1L)이 호출되면 이 Post를 돌려줘" 라고 Mock에게 시나리오를 주입
            Post post = Post.builder()
                    .postId(1L)
                    .writer("writer1")
                    .title("테스트의 기쁨")
                    .content("Mock을 알게 됐다")
                    .build();
            given(boardDao.findById(1L)).willReturn(post);

            // when
            PostResponse response = boardService.getPost(1L);

            // then : 상태 검증 — 반환값이 기대와 같은가
            assertThat(response.getPostId()).isEqualTo(1L);
            assertThat(response.getWriter()).isEqualTo("writer1");
            assertThat(response.getTitle()).isEqualTo("테스트의 기쁨");
        }

        @Test
        @DisplayName("없는 글이면 PostNotFoundException")
        void getPost_없는글_예외발생() {
            // given : Mock은 stubbing하지 않은 호출에 기본값(null)을 돌려준다 —
            //         여기서는 의도를 명확히 하기 위해 null을 명시적으로 stubbing
            given(boardDao.findById(99L)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> boardService.getPost(99L))
                    .isInstanceOf(PostNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("게시글 작성")
    class CreatePost {

        @Test
        @DisplayName("DAO에 작성자/제목이 채워진 Post가 전달되고, 채번된 ID를 반환한다")
        void createPost_정상요청_insert호출및생성ID반환() {
            // given : insert는 반환값이 없는 대신 "전달받은 Post에 ID를 채워넣는" 부수효과가 있다
            //         (실제로는 MyBatis useGeneratedKeys가 하는 일 — Mock으로 그 행동을 흉내낸다)
            willAnswer(invocation -> {
                Post saved = invocation.getArgument(0);
                saved.setPostId(10L);
                return null;
            }).given(boardDao).insert(any(Post.class));

            PostCreateRequest request = new PostCreateRequest("첫 글", "내용입니다");

            // when
            Long postId = boardService.createPost("writer1", request);

            // then(1) 상태 검증 : 채번된 ID가 그대로 반환됐는가
            assertThat(postId).isEqualTo(10L);

            // then(2) 행위 검증 : DAO에 "어떤 내용의" Post가 전달됐는지 캡처해서 확인
            //         반환값만 보면 "writer를 빠뜨리고 저장하는 버그"를 놓친다 — 그래서 Captor가 필요
            ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
            verify(boardDao).insert(captor.capture());

            Post savedPost = captor.getValue();
            assertThat(savedPost.getWriter()).isEqualTo("writer1");
            assertThat(savedPost.getTitle()).isEqualTo("첫 글");
        }
    }

    @Nested
    @DisplayName("게시글 수정 — 핵심 비즈니스 규칙: 작성자 본인만")
    class UpdatePost {

        @Test
        @DisplayName("작성자 본인이면 변경 내용으로 update를 호출한다")
        void updatePost_작성자본인_수정성공() {
            // given
            Post post = Post.builder()
                    .postId(1L).writer("writer1")
                    .title("원래 제목").content("원래 내용")
                    .build();
            given(boardDao.findById(1L)).willReturn(post);

            // when
            boardService.updatePost(1L, "writer1", new PostUpdateRequest("새 제목", "새 내용"));

            // then : 도메인의 상태가 바뀌었고(상태 검증), 그 상태로 update가 호출됐다(행위 검증)
            assertThat(post.getTitle()).isEqualTo("새 제목");
            verify(boardDao).update(post);
        }

        @Test
        @DisplayName("작성자가 아니면 예외가 발생하고, update는 절대 호출되지 않는다")
        void updatePost_작성자불일치_예외및수정안함() {
            // given
            Post post = Post.builder()
                    .postId(1L).writer("writer1")
                    .title("원래 제목").content("원래 내용")
                    .build();
            given(boardDao.findById(1L)).willReturn(post);

            // when & then
            assertThatThrownBy(() ->
                    boardService.updatePost(1L, "hacker", new PostUpdateRequest("탈취", "탈취")))
                    .isInstanceOf(NotPostOwnerException.class)
                    .hasMessageContaining("hacker");

            // 예외가 터진 것만으로는 부족하다 — "그 후 DB 변경이 시도되지 않았다"까지 증명해야
            // '예외는 던졌는데 update는 이미 실행된' 종류의 버그를 막을 수 있다
            then(boardDao).should(never()).update(any(Post.class));
        }
    }

    @Nested
    @DisplayName("게시글 삭제")
    class DeletePost {

        @Test
        @DisplayName("작성자 본인이면 삭제를 호출한다")
        void deletePost_작성자본인_삭제성공() {
            // given
            Post post = Post.builder().postId(1L).writer("writer1").title("t").content("c").build();
            given(boardDao.findById(1L)).willReturn(post);

            // when
            boardService.deletePost(1L, "writer1");

            // then
            verify(boardDao).deleteById(1L);
        }
    }
}
