package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

import org.jsoup.nodes.Document;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.CrawlerResult;

import java.time.LocalDate;

public interface Crawler {

    // 작성 일자가 null일 경우 기본값 설정
    // LocalDate.EPOCH(1970-01-01  - 시간이 없는 값 표현할 때 사용되는 관용적 기준점)
    // 이 값이 사용되면 작성 일자가 없는 것으로 간주
    LocalDate DEFAULT_DATE = LocalDate.EPOCH;

    boolean supports(String domain);
    CrawlerResult<?> extract(Document doc);
    LocalDate transLocalDate(String rawDate);
}
