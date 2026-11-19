package com.da.billiards.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Pages {

    private static final int MAX_SIZE = 48;

    private Pages() {
    }

    public static Pageable of(int page, int size, Sort sort) {
        int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, sort);
    }
}
