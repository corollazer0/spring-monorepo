package com.testonboarding.advanced.step11.example;

import com.testonboarding.board.dao.BoardDao;
import com.testonboarding.board.dto.PostResponse;
import com.testonboarding.board.dto.PostUpdateRequest;
import com.testonboarding.board.service.BoardService;
import com.testonboarding.common.exception.NotPostOwnerException;
import com.testonboarding.support.PostFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * [심화 Step 11 — example B] Fixture(Object Mother) 패턴으로 준비 코드 다이어트
 *
 * Before (Step 2 시절):
 *   Post post = Post.builder()
 *           .postId(1L).writer("writer1")
 *           .title("원래 제목").content("원래 내용")
 *           .build();                                  // 5줄 × 테스트 수만큼 반복...
 *
 * After:
 *   Post post = PostFixture.post(1L, "writer1");       // 이 테스트에서 중요한 것만!
 *
 * 효과는 "짧아짐"이 아니라 "신호 대 잡음비":
 * 테스트를 읽는 사람이 '이 시나리오에서 중요한 값'만 보게 된다.
 * 그리고 Post에 필드가 추가될 때 수정 지점이 Fixture 한 곳으로 모인다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Fixture 패턴 적용 (게시판 서비스)")
class FixtureRefactoringTest {

    @Mock
    private BoardDao boardDao;

    @InjectMocks
    private BoardService boardService;

    @Test
    @DisplayName("조회: 시나리오와 무관한 필드는 Fixture 기본값에 맡긴다")
    void getPost_Fixture사용_준비코드한줄() {
        // given : 이 테스트의 관심사는 "존재하는 글의 변환" — 제목/내용 값은 중요하지 않다
        given(boardDao.findById(1L)).willReturn(PostFixture.post(1L, "writer1"));

        // when
        PostResponse response = boardService.getPost(1L);

        // then
        assertThat(response.getPostId()).isEqualTo(1L);
        assertThat(response.getWriter()).isEqualTo("writer1");
    }

    @Test
    @DisplayName("소유자 검증: 이 시나리오에서 중요한 writer만 눈에 띈다")
    void updatePost_타인글_Fixture로작성자만강조() {
        // given : writer가 핵심인 시나리오 — Fixture 덕분에 writer만 도드라진다
        given(boardDao.findById(1L)).willReturn(PostFixture.post(1L, "writer1"));

        // when & then
        assertThatThrownBy(() ->
                boardService.updatePost(1L, "hacker", new PostUpdateRequest("탈취", "탈취")))
                .isInstanceOf(NotPostOwnerException.class);
    }

    @Test
    @DisplayName("특정 필드가 중요할 땐 builder로 그 필드만 덮어쓴다")
    void getPost_제목이중요한시나리오_부분덮어쓰기() {
        // given : 이번엔 제목이 검증 대상 — aPost()로 기본값 위에 제목만 교체
        given(boardDao.findById(1L)).willReturn(
                PostFixture.aPost().postId(1L).title("아주 특별한 제목").build());

        // when
        PostResponse response = boardService.getPost(1L);

        // then
        assertThat(response.getTitle()).isEqualTo("아주 특별한 제목");
    }
}
