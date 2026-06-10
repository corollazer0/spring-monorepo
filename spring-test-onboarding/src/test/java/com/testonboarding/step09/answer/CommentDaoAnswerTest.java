package com.testonboarding.step09.answer;

import com.testonboarding.comment.dao.CommentDao;
import com.testonboarding.comment.domain.Comment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Step 9 — answer] 댓글 DAO 테스트 (@MybatisTest)
 *
 * 전략: CommentMapper.xml의 SQL(등록순 정렬, IDENTITY 채번, 삭제)은
 * 진짜 DB에 실행해야 검증된다 → @MybatisTest 슬라이스.
 *
 * 시드: 1번 글에 댓글 2건(등록순 writer2 → writer1), 2번 글에 1건
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("댓글 DAO (캡스톤 모범답안)")
class CommentDaoAnswerTest {

    @Autowired
    private CommentDao commentDao;

    @Test
    @DisplayName("게시글의 댓글이 등록순으로 조회된다")
    void findByPostId_댓글목록_등록순반환() {
        // when
        List<Comment> comments = commentDao.findByPostId(1L);

        // then : 건수 + 순서(comment_id ASC = 등록순) 검증
        assertThat(comments).hasSize(2);
        assertThat(comments.get(0).getContent()).isEqualTo("첫 번째 댓글입니다");
        assertThat(comments.get(1).getContent()).isEqualTo("두 번째 댓글입니다");
    }

    @Test
    @DisplayName("댓글이 없는 글이면 빈 목록 (null이 아니다!)")
    void findByPostId_댓글없는글_빈목록() {
        // when : 3번 글에는 시드 댓글이 없다
        List<Comment> comments = commentDao.findByPostId(3L);

        // then : MyBatis의 컬렉션 반환은 비어있을 뿐 null이 아니다 — NPE 걱정 없는 계약 확인
        assertThat(comments).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("insert 후 PK가 채번되고 재조회로 왕복 검증된다")
    void insert_새댓글_채번및재조회() {
        // given
        Comment comment = Comment.builder()
                .postId(3L).writer("writer1").content("새 댓글")
                .build();

        // when
        commentDao.insert(comment);

        // then
        assertThat(comment.getCommentId()).isNotNull();
        Comment reloaded = commentDao.findById(comment.getCommentId());
        assertThat(reloaded.getContent()).isEqualTo("새 댓글");
        assertThat(reloaded.getCreatedAt()).isNotNull(); // DEFAULT GETDATE()
    }

    @Test
    @DisplayName("delete 후 조회하면 null")
    void deleteById_삭제후조회_null() {
        // when
        int affected = commentDao.deleteById(1L);

        // then
        assertThat(affected).isEqualTo(1);
        assertThat(commentDao.findById(1L)).isNull();
        // (이 삭제는 테스트 종료 시 롤백된다 — 다른 테스트에 영향 없음)
    }
}
