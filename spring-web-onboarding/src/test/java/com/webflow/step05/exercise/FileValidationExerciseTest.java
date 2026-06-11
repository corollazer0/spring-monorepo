package com.webflow.step05.exercise;

import com.webflow.file.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

/**
 * [Web Step 5 — exercise] 업로드 흐름을 직접 봉인해보세요
 *
 * 진행 방법:
 * 1. 클래스 위의 @Disabled 를 지운다
 * 2. TODO를 채운다 (힌트: example의 FileStorageServiceTest — MockMultipartFile 4인자 생성자)
 * 3. .\gradlew :spring-web-onboarding:test 로 통과를 확인한다
 */
@Disabled("과제: docs/web/education/FOR-WebFlow-Step05.md 참고 후 @Disabled를 제거하고 완성하세요")
@DisplayName("파일 검증 (연습문제)")
class FileValidationExerciseTest {

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
        // given & when : "photo.jpeg"라는 이름의 MockMultipartFile을 만들어 store 하세요
        // TODO 1

        // then : 반환된 이름이 .jpeg로 끝나고, tempDir에 그 파일이 실제로 존재하는지 검증하세요
        // TODO 2
    }

    @Test
    @DisplayName("이중 확장자 트릭(photo.jpg.exe)은 거부된다")
    void store_이중확장자_거부() {
        // when & then : "photo.jpg.exe"를 store 하면 InvalidFileException이 터지는지 검증하세요
        //               (확장자 추출은 "마지막 점" 기준 — exe로 판정되는 것이 올바른 동작!)
        // TODO 3
    }

    @Test
    @DisplayName("저장 후 같은 이름으로 읽으면 내용이 보존된다 (왕복)")
    void store_저장후읽기_내용보존() throws Exception {
        // given & when : 내용이 있는 png를 store 하고, 반환된 이름으로 loadAsResource 하세요
        // TODO 4

        // then : resource.contentLength()가 보낸 바이트 수와 같은지 검증하세요
        // TODO 5
    }
}
