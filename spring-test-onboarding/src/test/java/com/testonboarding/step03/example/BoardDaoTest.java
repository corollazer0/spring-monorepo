package com.testonboarding.step03.example;

import com.testonboarding.board.dao.BoardDao;
import com.testonboarding.board.domain.Post;
import com.testonboarding.board.dto.PostSearchCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Step 3 — example] DAO 테스트: @MybatisTest + H2(MSSQLServer 모드)
 *
 * 문제 상황: Step 2의 Mock은 XML 속 SQL을 쳐다보지도 않는다.
 * WHERE 절 오타, 컬럼-필드 매핑 실수, 페이징 문법 오류는 진짜 DB에 SQL을 날려야만 잡힌다.
 *
 * 해결: @MybatisTest 슬라이스 테스트
 * - 전체 애플리케이션이 아니라 "MyBatis + DataSource"만 뜬다 → @SpringBootTest보다 훨씬 빠름
 * - schema.sql / data.sql이 자동 실행되어 매 테스트가 같은 데이터에서 시작
 * - 각 테스트는 트랜잭션 안에서 실행되고 끝나면 자동 롤백 → 테스트끼리 데이터 간섭 없음
 *
 * ⚠️ 함정 주의: @AutoConfigureTestDatabase(replace = Replace.NONE)
 * 이게 없으면 Spring이 우리 설정(MSSQLServer 모드 URL)을 무시하고
 * 기본 H2로 바꿔치기한다 → MS-SQL 문법(OFFSET/FETCH 등)이 실패한다!
 *
 * 시드 데이터(data.sql): 회원 3명, 게시글 15건(제목에 '테스트' 포함 3건)
 */
@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("게시글 DAO (@MybatisTest)")
class BoardDaoTest {

    @Autowired
    private BoardDao boardDao;

    @Nested
    @DisplayName("등록 — IDENTITY 채번과 DEFAULT 컬럼")
    class Insert {

        @Test
        @DisplayName("insert 후 DB가 채번한 PK가 객체에 채워진다 (useGeneratedKeys)")
        void insert_새글_PK채번및기본값확인() {
            // given
            Post post = Post.builder()
                    .writer("writer1")
                    .title("새 글")
                    .content("내용")
                    .build();
            assertThat(post.getPostId()).isNull(); // 아직 PK 없음

            // when
            boardDao.insert(post);

            // then(1) : IDENTITY가 채번한 PK가 useGeneratedKeys로 객체에 채워졌다
            assertThat(post.getPostId()).isNotNull();

            // then(2) : 다시 조회해서 "진짜 저장됐는지" + DEFAULT GETDATE()가 동작했는지 확인
            Post found = boardDao.findById(post.getPostId());
            assertThat(found.getTitle()).isEqualTo("새 글");
            assertThat(found.getCreatedAt()).isNotNull(); // INSERT문에 없었지만 DB가 채움
        }
    }

    @Nested
    @DisplayName("페이징 — MS-SQL OFFSET/FETCH")
    class FindPage {

        @Test
        @DisplayName("1페이지(offset 0)는 최신글부터 10건")
        void findPage_1페이지_최신순10건() {
            // when
            List<Post> page = boardDao.findPage(0, 10);

            // then : 시드 15건 중 10건, post_id 내림차순(최신글 먼저)
            assertThat(page).hasSize(10);
            assertThat(page.get(0).getPostId())
                    .as("첫 번째 행이 가장 큰 post_id(최신글)여야 한다")
                    .isGreaterThan(page.get(9).getPostId());
        }

        @Test
        @DisplayName("2페이지(offset 10)는 남은 5건")
        void findPage_2페이지_남은5건() {
            // when
            List<Post> page = boardDao.findPage(10, 10);

            // then : 15건 - 10건 = 5건
            assertThat(page).hasSize(5);
        }
    }

    @Nested
    @DisplayName("동적 검색 — <where> + <if>")
    class Search {

        @Test
        @DisplayName("제목 키워드 검색: '테스트'가 들어간 글만 반환")
        void search_제목키워드_해당글만반환() {
            // given
            PostSearchCondition condition = new PostSearchCondition("테스트", null);

            // when
            List<Post> result = boardDao.search(condition);

            // then : 시드 데이터에 '테스트' 제목 글은 정확히 3건
            assertThat(result).hasSize(3);
            assertThat(result).allSatisfy(post ->
                    assertThat(post.getTitle()).contains("테스트"));
        }

        @Test
        @DisplayName("제목 + 작성자 복합 조건")
        void search_복합조건_모두만족하는글만반환() {
            // given : '테스트' 제목 3건 중 writer1이 쓴 글은 2건
            PostSearchCondition condition = new PostSearchCondition("테스트", "writer1");

            // when
            List<Post> result = boardDao.search(condition);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).allSatisfy(post -> {
                assertThat(post.getTitle()).contains("테스트");
                assertThat(post.getWriter()).isEqualTo("writer1");
            });
        }

        @Test
        @DisplayName("조건이 모두 비어있으면 전체 조회 (<where>가 통째로 빠진다)")
        void search_조건없음_전체반환() {
            // when
            List<Post> result = boardDao.search(new PostSearchCondition(null, null));

            // then
            assertThat(result).hasSize(15);
        }
    }

    @Nested
    @DisplayName("수정/삭제")
    class UpdateAndDelete {

        @Test
        @DisplayName("update 후 다시 조회하면 변경이 반영되어 있다")
        void update_제목변경_반영확인() {
            // given : 시드 글 하나를 가져와 수정
            Post post = boardDao.findById(1L);
            post.change("수정된 제목", "수정된 내용");

            // when
            int affected = boardDao.update(post);

            // then : 영향받은 행 수 + 재조회 검증 (객체가 아니라 "DB"가 바뀌었는지 봐야 한다)
            assertThat(affected).isEqualTo(1);
            Post reloaded = boardDao.findById(1L);
            assertThat(reloaded.getTitle()).isEqualTo("수정된 제목");
        }

        @Test
        @DisplayName("delete 후 조회하면 null")
        void deleteById_삭제후조회_null반환() {
            // when
            int affected = boardDao.deleteById(1L);

            // then
            assertThat(affected).isEqualTo(1);
            assertThat(boardDao.findById(1L)).isNull();
        }

        /**
         * 위 테스트가 1번 글을 지웠는데 이 테스트는 어떻게 15건을 보장받을까?
         * → @MybatisTest는 테스트마다 트랜잭션을 열고 끝나면 롤백한다.
         *   모든 테스트는 항상 "시드 데이터 그대로"에서 시작한다 (테스트 격리).
         */
        @Test
        @DisplayName("롤백 확인: 이전 테스트의 삭제가 이 테스트에 영향을 주지 않는다")
        void count_롤백덕분에_항상시드그대로() {
            assertThat(boardDao.count()).isEqualTo(15L);
        }
    }
}
