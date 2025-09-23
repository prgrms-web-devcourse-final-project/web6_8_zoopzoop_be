package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

import org.jsoup.nodes.Document;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.tuna.zoopzoop.backend.domain.datasource.dto.ArticleData;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NaverNewsCrawler implements Crawler {
    private static final SupportedDomain DOMAIN = SupportedDomain.NAVERNEWS;

    @Override
    public boolean supports(String domain) {
        return domain.contains(DOMAIN.getDomain());
    }

    @Override
    public ArticleData extract(Document doc) {
        // 제목
        String title = doc.select("h2").text();

        // 작성 날짜
        String publishedAt = doc.selectFirst(
                "span.media_end_head_info_datestamp_time._ARTICLE_DATE_TIME"
        ).attr("data-date-time").split(" ")[0];

        // 내용(ai한테 줘야함)
        String content = doc.select("article").text();

        // 썸네일 이미지 url
        String imgUrl = doc.selectFirst("img#img1._LAZY_LOADING._LAZY_LOADING_INIT_HIDE").attr("data-src");

        // 카테고리

        // 태그

        return new ArticleData(title, publishedAt, content, imgUrl, null);
    }
}
