package com.webflow.step05.example;

import com.webflow.common.exception.InvalidFileException;
import com.webflow.common.exception.StoredFileNotFoundException;
import com.webflow.file.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [Web Step 5 — example A] 파일 저장소 — 순수 단위 테스트 + @TempDir
 *
 * Spring 컨텍스트가 전혀 없다! FileStorageService는 프로퍼티(경로)를 생성자로 받는
 * 평범한 클래스 — @TempDir(테스트마다 새로 만들어지고 자동 삭제되는 임시 폴더)를
 * 꽂으면 진짜 파일 I/O를 디스크 오염 없이 검증할 수 있다.
 *
 * 업로드 파일은 MockMultipartFile로 — (파트명, 원본 파일명, 컨텐츠 타입, 바이트).
 */
@DisplayName("파일 저장소")
class FileStorageServiceTest {

    @TempDir
    Path tempDir;   // 테스트마다 격리된 새 폴더 — 실행 순서 의존이 생길 수 없다

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    private MockMultipartFile imageFile(String originalName, String content) {
        return new MockMultipartFile("image", originalName, "image/jpeg",
                content.getBytes(StandardCharsets.UTF_8));
    }

    @Nested
    @DisplayName("저장 (검증 + 쓰기)")
    class Store {

        @Test
        @DisplayName("정상 이미지: 서버 생성 이름(UUID.jpg)으로 저장되고 내용이 보존된다")
        void store_정상이미지_저장성공() throws IOException {
            // when
            String storedName = fileStorageService.store(imageFile("photo.jpg", "fake-image-bytes"));

            // then : 반환된 이름으로 실제 파일이 존재하고, 내용이 같다
            assertThat(storedName).endsWith(".jpg")
                    .isNotEqualTo("photo.jpg");   // 사용자가 준 이름을 그대로 쓰지 않는다!
            Path stored = tempDir.resolve(storedName);
            assertThat(stored).exists();
            assertThat(new String(Files.readAllBytes(stored), StandardCharsets.UTF_8))
                    .isEqualTo("fake-image-bytes");
        }

        @Test
        @DisplayName("대문자 확장자(PHOTO.PNG)도 허용 — 검증은 소문자 변환 후")
        void store_대문자확장자_허용() {
            // when
            String storedName = fileStorageService.store(imageFile("PHOTO.PNG", "png-bytes"));

            // then
            assertThat(storedName).endsWith(".png");
        }

        @Test
        @DisplayName("화이트리스트 밖 확장자(exe)는 거부 — 그리고 디스크에 아무것도 안 남는다")
        void store_허용외확장자_거부() {
            // when & then
            assertThatThrownBy(() -> fileStorageService.store(imageFile("malware.exe", "MZ...")))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessageContaining("확장자");

            // 거부됐다면 부수효과(파일)도 없어야 한다 — 검증이 쓰기보다 먼저라는 증거
            assertThat(tempDir.toFile().listFiles()).isEmpty();
        }

        @Test
        @DisplayName("빈 파일은 거부")
        void store_빈파일_거부() {
            MockMultipartFile empty = new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);

            assertThatThrownBy(() -> fileStorageService.store(empty))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessageContaining("빈 파일");
        }

        @Test
        @DisplayName("확장자 없는 파일명도 거부 (null 확장자)")
        void store_확장자없음_거부() {
            assertThatThrownBy(() -> fileStorageService.store(imageFile("noextension", "data")))
                    .isInstanceOf(InvalidFileException.class);
        }
    }

    @Nested
    @DisplayName("읽기 (경로 방어)")
    class Load {

        @Test
        @DisplayName("저장한 파일을 Resource로 되읽는다 — 저장↔읽기 왕복 검증")
        void loadAsResource_저장후읽기_왕복() throws IOException {
            // given
            String storedName = fileStorageService.store(imageFile("photo.jpg", "round-trip"));

            // when
            Resource resource = fileStorageService.loadAsResource(storedName);

            // then
            assertThat(resource.exists()).isTrue();
            assertThat(resource.contentLength()).isEqualTo("round-trip".length());
        }

        @Test
        @DisplayName("없는 파일 → StoredFileNotFoundException (404 계열)")
        void loadAsResource_없는파일_예외() {
            assertThatThrownBy(() -> fileStorageService.loadAsResource("ghost.jpg"))
                    .isInstanceOf(StoredFileNotFoundException.class);
        }

        @Test
        @DisplayName("경로 탈출(../) 시도는 차단 — 업로드 폴더 밖은 읽을 수 없다")
        void loadAsResource_경로탈출_차단() {
            // when & then : ../로 상위 폴더의 파일을 노리는 고전 공격 — normalize 후 경계 검증
            assertThatThrownBy(() -> fileStorageService.loadAsResource("../secret.txt"))
                    .isInstanceOf(InvalidFileException.class)
                    .hasMessageContaining("경로");
        }
    }
}
