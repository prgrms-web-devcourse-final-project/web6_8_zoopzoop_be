package org.tuna.zoopzoop.backend.domain.datasource.dataprocessor.service;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.datasource.ai.dto.AiExtractorDto;
import org.tuna.zoopzoop.backend.domain.datasource.ai.dto.AnalyzeContentDto;
import org.tuna.zoopzoop.backend.domain.datasource.ai.service.AiService;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.CrawlerResult;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.SpecificSiteDto;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.UnspecificSiteDto;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.service.CrawlerManagerService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.ArticleData;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceDto;
import org.tuna.zoopzoop.backend.domain.datasource.entity.Tag;
import org.tuna.zoopzoop.backend.domain.datasource.exception.ServiceException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DataProcessorService {
    public final CrawlerManagerService crawlerManagerService;
    public final AiService aiService;

    /**
        url체크 메소드
        - 도메인에 https, http 둘 다 붙이기
        - https, http 둘 다 실행
     */
    public Map<String, Object> checkUrl(String inputUrl) {
        List<String> candidatesUrl = new ArrayList<>();

        // 1. 원본에서 프로토콜 제거
        String strippedUrl = inputUrl.replaceFirst("^https?://", "");

        // 2. 무조건 두 가지 프로토콜을 모두 리스트에 추가
        candidatesUrl.add("https://" + strippedUrl);
        candidatesUrl.add("http://" + strippedUrl);

        for (String url : candidatesUrl) {
            try {
                Connection.Response response = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0")
                        .timeout(5000)
                        .followRedirects(true)
                        .execute();

                Document doc = response.parse();
                return Map.of(
                        "url", url,
                        "document", doc
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 모든 시도 실패 시 마지막 예외 던지기
        throw new ServiceException("URL 접속에 실패했습니다.");
    }

    public DataSourceDto process(String inputUrl, List<Tag> tagList) {
        Map<String, Object> outputMap = checkUrl(inputUrl);
        String url = (String) outputMap.get("url");
        Document doc = (Document) outputMap.get("document");

        CrawlerResult<?> result = crawlerManagerService.extractContent(url, doc);

        ArticleData articleData = switch (result.type()) {
            case SPECIFIC -> {
                SpecificSiteDto specificSiteDto = (SpecificSiteDto) result.data();
                yield new ArticleData(
                        specificSiteDto.title(),
                        specificSiteDto.content(),
                        specificSiteDto.dataCreatedDate(),
                        specificSiteDto.imageUrl(),
                        specificSiteDto.source()
                );
            }
            case UNSPECIFIC -> {
                UnspecificSiteDto unspecificSiteDto = (UnspecificSiteDto) result.data();
                AiExtractorDto aiExtractorDto = aiService.extract(unspecificSiteDto.rawHtml());
                yield new ArticleData(
                        aiExtractorDto.title(),
                        aiExtractorDto.content(),
                        aiExtractorDto.dataCreatedDate(),
                        aiExtractorDto.imageUrl(),
                        aiExtractorDto.source()
                );
            }
        };

        AnalyzeContentDto analyzeContentDto = aiService.analyzeContent(articleData.content(), tagList);

        return new DataSourceDto(
                articleData.title(),
                analyzeContentDto.summary(),
                articleData.dataCreatedDate(),
                url,
                articleData.imageUrl(),
                articleData.source(),
                analyzeContentDto.category(),
                analyzeContentDto.tags()
        );
    }
}
