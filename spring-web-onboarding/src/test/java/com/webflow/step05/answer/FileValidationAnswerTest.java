package com.webflow.step05.answer;

import com.webflow.common.exception.InvalidFileException;
import com.webflow.file.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * [Web Step 5 — answer] FileValidationExerciseTest 모범답안
 *
 * 채점 포인트: 이중 확장자(TODO 3)를 "마지막 점 기준 exe 판정 = 올바른 동작"으로
 * 이해했는가 — 화이트리스트는 첫 확장자가 아니라 실제 실행 확장자를 본다.
 */
@DisplayName("파일 검증 (모범답안)")
class FileValidationAnswerTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString());
    }

    @Test
    @DisplayName("jpeg 확장자도 허용 목록에 있다 (jpg와 별개 문자열!)")
    void store_jpeg확장자_허용() {
        // given & when (TODO 1 답)
        String storedName = fileStorageService.store(new MockMultipartFile(
                "image", "photo.jpeg", "image/jpeg", "bytes".getBytes()));

        // then (TODO 2 답)
        assertThat(storedName).endsWith(".jpeg");
        assertThat(tempDir.resolve(storedName)).exists();
    }

    @Test
    @DisplayName("이중 확장자 트릭(photo.jpg.exe)은 거부된다")
    void store_이중확장자_거부() {
        // when & then (TODO 3 답) : 마지막 점 기준이라 exe로 판정 → 거부가 정답
        assertThatThrownBy(() -> fileStorageService.store(new MockMultipartFile(
                "image", "photo.jpg.exe", "image/jpeg", "MZ".getBytes())))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    @DisplayName("저장 후 같은 이름으로 읽으면 내용이 보존된다 (왕복)")
    void store_저장후읽기_내용보존() throws Exception {
        // given & when (TODO 4 답)
        byte[] bytes = "png-content".getBytes();
        String storedName = fileStorageService.store(new MockMultipartFile(
                "image", "photo.png", "image/png", bytes));
        Resource resource = fileStorageService.loadAsResource(storedName);

        // then (TODO 5 답)
        assertThat(resource.contentLength()).isEqualTo(bytes.length);
    }
}
