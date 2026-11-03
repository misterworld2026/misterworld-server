package com.misterworld.server.controller;

import org.springframework.data.domain.*;

import static com.misterworld.server.common.BusinessException.require;

public final class Pages {
    private Pages() {
    }

    public static Pageable of(int page, int size, String sort) {
        require(page >= 0 && size >= 1 && size <= 100, "page는 0 이상, size는 1~100입니다.");
        return PageRequest.of(page, size, Sort.by(sort).ascending());
    }
}
