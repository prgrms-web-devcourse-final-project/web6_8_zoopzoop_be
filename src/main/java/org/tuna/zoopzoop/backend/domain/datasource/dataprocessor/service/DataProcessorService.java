package org.tuna.zoopzoop.backend.domain.datasource.dataprocessor.service;

import lombok.RequiredArgsConstructor;
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

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DataProcessorService {
    public final CrawlerManagerService crawlerManagerService;
    public final AiService aiService;

    public DataSourceDto process(String url, List<Tag> tagList) throws IOException {
        CrawlerResult<?> result = crawlerManagerService.extractContent(url);

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
