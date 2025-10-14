package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

import org.jsoup.nodes.Document;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.CrawlerResult;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.UnspecificSiteDto;

import java.time.LocalDate;

@Component
@Order(Ordered.LOWEST_PRECEDENCE) // 모든 URL 대응 (우선순위 맨 뒤)
public class GenericCrawler implements Crawler {
    @Override
    public boolean supports(String url) {
        return true;
    }

    @Override
    public CrawlerResult<?> extract(Document doc) {
        // img 태그
        doc.select("img[src]").forEach(el ->
                el.attr("src", el.absUrl("src"))
        );

        // meta 태그 (Open Graph, Twitter Card 등)
        doc.select("meta[content]").forEach(meta -> {
            String absUrl = meta.absUrl("content");
            if (!absUrl.isEmpty() && !absUrl.equals(meta.attr("content"))) {
                meta.attr("content", absUrl);
            }
        });

        // 본문만 가져오기 (HTML)
        String cleanHtml = doc.body().html()
                .replaceAll("<script[^>]*>.*?</script>", "")
                .replaceAll("<style[^>]*>.*?</style>", "")
                // 주석 제거
                .replaceAll("<!--.*?-->", "")
                // 연속된 공백 제거
                .replaceAll("\\s+", " ")
                // 불필요한 속성 제거
                .replaceAll("(class|id|style|onclick|onload)=\"[^\"]*\"", "")
                .trim();

        return new CrawlerResult<>(
                CrawlerResult.CrawlerType.UNSPECIFIC,
                new UnspecificSiteDto(cleanHtml)
        );
    }

    @Override
    public LocalDate transLocalDate(String rawDate) {
        return null;
    }
}
