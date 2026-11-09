package com.misterworld.server.common;

public record ResultDto(
    boolean success,
    String message,
    int status,
    String code
) {
}
