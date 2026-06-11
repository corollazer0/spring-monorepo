package com.webflow.common.exception;

/**
 * 저장된 파일 없음 (이미지 미등록, 디스크에서 유실) — 404 계열.
 */
public class StoredFileNotFoundException extends RuntimeException {

    public StoredFileNotFoundException(String message) {
        super(message);
    }
}
