package com.pacto.api.common.dto;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final int totalPages;
    private final int currentPage;

    private PageResponse(List<T> content, int totalPages, int currentPage) {
        this.content = content;
        this.totalPages = totalPages;
        this.currentPage = currentPage;
    }

    public static <S, T> PageResponse<T> from(Page<S> page, Function<S, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getTotalPages(),
                page.getNumber() + 1
        );
    }
}
