package com.misterworld.server.common;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int status;
    private final String code;

    public BusinessException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static BusinessException notFound() {
        return new BusinessException(404, "NOT_FOUND", "대상을 찾을 수 없습니다.");
    }

    public static BusinessException policy() {
        return new BusinessException(409, "POLICY_UNRESOLVED", "정책 결정이 필요합니다. docs/REVIEW.md를 확인하세요.");
    }

    public static void require(boolean valid, String message) {
        if (!valid)
            throw new BusinessException(400, "INVALID_REQUEST", message);
    }
}
