package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.CrawlerResult;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.SpecificSiteDto;
import org.tuna.zoopzoop.backend.domain.datasource.exception.ServiceException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class NaverBlogCrawler implements Crawler {
    private static final SupportedDomain DOMAIN = SupportedDomain.NAVERBLOG;
    private static final DateTimeFormatter NAVERBLOG_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy. M. d. HH:mm");

    @Override
    public boolean supports(String domain) {
        return domain.contains(DOMAIN.getDomain());
    }

    @Override
    public CrawlerResult<?> extract(Document doc) {
        /*
            블로그 본문은 <iframe id="mainFrame"> 안에 로드되므로
            먼저 메인 페이지를 가져온 뒤 iframe의 src를 추출하여
            해당 URL로 다시 connect 해야 실제 본문 내용을 크롤링할 수 있다.
         */
        String iframeUrl = Optional.ofNullable(doc.selectFirst("iframe#mainFrame"))
                .map(el -> el.absUrl("src"))
                .orElse("");

        Document iframeDoc;

        try {
            Connection.Response response = Jsoup.connect(iframeUrl)
                    .userAgent("Mozilla/5.0")
                    .timeout(10000)
                    .followRedirects(true)
                    .execute();

            iframeDoc = response.parse();
        } catch (Exception e) {
            throw new ServiceException("URL 접속에 실패했습니다.");
        }

        // 제목
        String title = iframeDoc.select("meta[property=og:title]").attr("content");


        // 작성일자
        String publishedAt = Optional.ofNullable(iframeDoc.selectFirst("span.se_publishDate.pcol2"))
                .map(Element::text)
                .orElse("");
        LocalDate dataCreatedDate = transLocalDate(publishedAt);


        // 내용
        Elements spans = iframeDoc.select(".se-main-container span");
        StringBuilder sb = new StringBuilder();
        for (Element span : spans) {
            sb.append(span.text()); // 태그 안 텍스트만
        }
        String content = sb.toString();


        // 썸네일 이미지 URL
        String imageUrl = iframeDoc.select("meta[property=og:image]").attr("content");


        // 출처
        String source = iframeDoc.select("meta[property=og:site_name]").attr("content");

        return new CrawlerResult<>(
                CrawlerResult.CrawlerType.SPECIFIC,
                new SpecificSiteDto(title, dataCreatedDate, content, imageUrl, source)
        );
    }

    @Override
    public LocalDate transLocalDate(String publishedAt) {
        if (publishedAt == null || publishedAt.isEmpty()) {
            return null;
        }

        publishedAt = publishedAt.trim();

        // "방금 전"
        if (publishedAt.equals("방금 전")) {
            return LocalDate.now();
        }

        // "?분 전"
        Pattern minutePattern = Pattern.compile("(\\d+)분\\s*전");
        Matcher minuteMatcher = minutePattern.matcher(publishedAt);
        if (minuteMatcher.find()) {
            int minutes = Integer.parseInt(minuteMatcher.group(1));
            LocalDateTime pastTime = LocalDateTime.now().minusMinutes(minutes);
            return pastTime.toLocalDate();
        }

        // "?시간 전"
        Pattern hourPattern = Pattern.compile("(\\d+)시간\\s*전");
        Matcher hourMatcher = hourPattern.matcher(publishedAt);
        if (hourMatcher.find()) {
            int hours = Integer.parseInt(hourMatcher.group(1));
            LocalDateTime pastTime = LocalDateTime.now().minusHours(hours);
            return pastTime.toLocalDate();
        }

        // "yyyy. M. d. HH:mm" 또는 "yyyy. M. d. H:mm" 형식
        try {
            // 시간 부분 제거하고 날짜만 추출
            Pattern datePattern = Pattern.compile("(\\d{4})\\s*\\.\\s*(\\d{1,2})\\s*\\.\\s*(\\d{1,2})");
            Matcher dateMatcher = datePattern.matcher(publishedAt);

            if (dateMatcher.find()) {
                int year = Integer.parseInt(dateMatcher.group(1));
                int month = Integer.parseInt(dateMatcher.group(2));
                int day = Integer.parseInt(dateMatcher.group(3));
                return LocalDate.of(year, month, day);
            }
        } catch (Exception e) {
            System.err.println("날짜 파싱 실패: " + publishedAt);
        }

        // 파싱 실패 시 null 반환
        return null;
    }
}
