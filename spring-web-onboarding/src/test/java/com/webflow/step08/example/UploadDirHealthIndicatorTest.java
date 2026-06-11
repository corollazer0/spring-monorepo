package com.webflow.step08.example;

import com.webflow.common.health.UploadDirHealthIndicator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [Web Step 8 — example A] HealthIndicator는 순수 단위로 — UP과 DOWN 양면
 *
 * health()의 규약: 어떤 상황에서도 예외 없이 UP/DOWN "값"으로 답한다.
 * DOWN 케이스를 어떻게 만들까? — "경로 자리에 파일을 놓는다" (createDirectories가
 * 실패하는 결정적이고 OS 무관한 방법). 장애 연출도 테스트 설계다.
 */
@DisplayName("업로드 디렉터리 헬스 체크")
class UploadDirHealthIndicatorTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("쓰기 가능한 디렉터리 → UP + 경로 상세")
    void health_정상디렉터리_UP() {
        // given
        UploadDirHealthIndicator indicator =
                new UploadDirHealthIndicator(tempDir.toString());

        // when
        Health health = indicator.health();

        // then : 상태와 함께 "어디를 봤는지"(path 상세)도 계약이다
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("path");
    }

    @Test
    @DisplayName("디렉터리가 없으면 만들어서라도 UP — 자가 복구형 체크")
    void health_없는디렉터리_생성후UP() {
        // given : 아직 존재하지 않는 하위 경로
        Path notYet = tempDir.resolve("sub/upload");
        UploadDirHealthIndicator indicator = new UploadDirHealthIndicator(notYet.toString());

        // when
        Health health = indicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(notYet).exists();
    }

    @Test
    @DisplayName("경로 자리에 파일이 버티고 있으면 → DOWN (예외가 아니라 값으로!)")
    void health_경로가파일_DOWN() throws Exception {
        // given : 디렉터리를 만들 수 없는 상황 연출 — 그 이름의 "파일"을 먼저 만든다
        Path occupied = tempDir.resolve("upload");
        Files.createFile(occupied);
        UploadDirHealthIndicator indicator = new UploadDirHealthIndicator(occupied.toString());

        // when : 예외가 던져지지 않는 것 자체가 첫 번째 검증이다
        Health health = indicator.health();

        // then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("path");
    }
}
