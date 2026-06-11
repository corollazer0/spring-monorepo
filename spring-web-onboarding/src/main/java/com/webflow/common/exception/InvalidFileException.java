package com.webflow.common.exception;

/**
 * 업로드 파일 검증 실패 (빈 파일, 허용 외 확장자, 경로 탈출 시도) — 400 계열.
 * BusinessException 상속 → 별도 핸들러 없이 부모 매핑(400)을 탄다.
 */
public class InvalidFileException extends BusinessException {

    public InvalidFileException(String reason) {
        super("파일을 처리할 수 없습니다. 사유: " + reason);
    }
}
