package com.webflow.file;

import com.webflow.common.exception.InvalidFileException;
import com.webflow.common.exception.StoredFileNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 파일 저장소 서비스 — 로컬 디스크 구현.
 *
 * 보안 3원칙:
 * 1. 확장자 화이트리스트 — "위험한 것 차단(블랙리스트)"이 아니라 "안전한 것만 허용"
 * 2. 저장 파일명은 서버가 생성(UUID) — 사용자가 준 이름을 경로에 쓰지 않는다
 * 3. 읽기 경로는 업로드 디렉터리 안인지 검증 — ../ 경로 탈출 차단
 *
 * 저장 위치는 프로퍼티(app.upload-dir) 주입 — 테스트는 @TempDir로 갈아끼운다.
 */
@Slf4j
@Service
public class FileStorageService {

    /** 화이트리스트 — 여기 없는 확장자는 전부 거부 (exe, jsp, html, svg ...) */
    private static final Set<String> ALLOWED_EXTENSIONS =
            new HashSet<>(Arrays.asList("jpg", "jpeg", "png"));

    private final Path uploadDir;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * 검증 후 저장하고, 서버가 생성한 파일명을 반환한다.
     */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("빈 파일입니다");
        }

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new InvalidFileException(
                    "허용되지 않는 확장자입니다: " + extension + " (허용: " + ALLOWED_EXTENSIONS + ")");
        }

        // 사용자가 보낸 이름은 버린다 — 서버 생성 이름만 경로가 된다 (인젝션 원천 차단)
        String storedName = UUID.randomUUID() + "." + extension.toLowerCase(Locale.ROOT);
        try {
            Files.createDirectories(uploadDir);
            Files.copy(file.getInputStream(), uploadDir.resolve(storedName),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장 실패: " + storedName, e);
        }

        log.info(">>>>> [FileStorageService] 저장 완료. original={}, stored={}",
                file.getOriginalFilename(), storedName);
        return storedName;
    }

    /**
     * 저장된 파일을 Resource로 — 경로 탈출(../)과 유실을 여기서 막는다.
     */
    public Resource loadAsResource(String filename) {
        Path target = uploadDir.resolve(filename).normalize();

        // 경로 탈출 방어: 최종 경로가 업로드 디렉터리 "안"인가
        if (!target.startsWith(uploadDir)) {
            throw new InvalidFileException("잘못된 파일 경로입니다: " + filename);
        }
        if (!Files.exists(target)) {
            throw new StoredFileNotFoundException("파일을 찾을 수 없습니다: " + filename);
        }
        return new FileSystemResource(target);
    }
}
