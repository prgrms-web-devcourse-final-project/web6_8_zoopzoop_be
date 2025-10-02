package org.tuna.zoopzoop.backend.domain.space.space.dto.res;

import lombok.Getter;
import org.springframework.data.domain.Page;
import org.tuna.zoopzoop.backend.domain.space.space.dto.etc.SpaceInfo;

import java.util.List;

@Getter
public class ResBodyForSpaceListPage {
    private final List<SpaceInfo> spaces; // 현재 페이지의 데이터
    private final int page;          // 현재 페이지 번호 (0부터 시작)
    private final int size;          // 페이지 크기
    private final long totalElements; // 전체 요소 수
    private final int totalPages;    // 전체 페이지 수
    private final boolean isLast;      // 마지막 페이지 여부

    public ResBodyForSpaceListPage(Page<SpaceInfo> page) {
        this.spaces = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.isLast = page.isLast();
    }
}