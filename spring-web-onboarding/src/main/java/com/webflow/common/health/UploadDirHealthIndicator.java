package com.webflow.common.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 업로드 디렉터리 헬스 체크 (Step 8) — 커스텀 HealthIndicator.
 *
 * DB는 Boot가 알아서 봐주지만(DataSourceHealthIndicator 자동 등록),
 * "우리 서비스만의 의존물"(업로드 디스크)은 우리가 알려줘야 한다.
 * 빈 이름 uploadDirHealthIndicator → /actuator/health의 components.uploadDir로 노출.
 *
 * 규약: health()는 절대 예외를 던지지 않는다 — 무슨 일이 있어도 UP/DOWN "값"으로
 * 답한다 (헬스 체크가 터지면 모니터링 자체가 눈을 잃는다).
 */
@Slf4j
@Component
public class UploadDirHealthIndicator implements HealthIndicator {

    private final String uploadDir;

    public UploadDirHealthIndicator(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public Health health() {
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);   // 없으면 만든다 — 자가 복구형 체크

            if (Files.isWritable(dir)) {
                return Health.up()
                        .withDetail("path", dir.toString())
                        .build();
            }
            return Health.down()
                    .withDetail("path", dir.toString())
                    .withDetail("reason", "쓰기 권한 없음")
                    .build();
        } catch (Exception e) {
            // 디렉터리 생성 실패(경로가 파일이거나, 디스크 문제 등) — DOWN + 원인
            log.warn(">>>>> [WARN] 업로드 디렉터리 헬스 체크 실패: {}", e.getMessage());
            return Health.down(e)
                    .withDetail("path", uploadDir)
                    .build();
        }
    }
}
