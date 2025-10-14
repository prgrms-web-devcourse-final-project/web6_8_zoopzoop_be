package org.tuna.zoopzoop.backend.domain.datasource.crawler.dto;

import java.time.LocalDate;

public record SpecificSiteDto(
        String title, // 제목
        LocalDate dataCreatedDate, // 작성일자
        String content, // ai한테 줘야할 내용
        String imageUrl, // 썸네일 이미지 url
        String source // 출처
) {
    @Override
    public String toString() {
        return "SpecificSiteDto {\n" +
                "  title='" + title + "',\n" +
                "  dataCreatedDate=" + dataCreatedDate + ",\n" +
                "  content='" + content + "',\n" +
                "  imageUrl='" + imageUrl + "',\n" +
                "  source='" + source + "'\n" +
                "}";
    }
}
