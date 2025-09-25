package org.tuna.zoopzoop.backend.domain.datasource.dataprocessor.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.tuna.zoopzoop.backend.domain.datasource.ai.dto.AiExtractorDto;
import org.tuna.zoopzoop.backend.domain.datasource.ai.service.AiService;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.CrawlerResult;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.SpecificSiteDto;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.dto.UnspecificSiteDto;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.service.CrawlerManagerService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.ArticleData;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class DataProcessorService {
    public final CrawlerManagerService crawlerManagerService;
    public final AiService aiService;

    public ArticleData process(String url) throws IOException {
        CrawlerResult<?> result = crawlerManagerService.extractContent(url);

        return switch (result.type()) {
            case SPECIFIC -> {
                SpecificSiteDto specificSiteDto = (SpecificSiteDto) result.data();
                yield new ArticleData(
                        specificSiteDto.title(),
                        specificSiteDto.dataCreatedDate(),
                        specificSiteDto.content(),
                        specificSiteDto.imageUrl(),
                        specificSiteDto.sources()
                );
            }
            case UNSPECIFIC -> {
                UnspecificSiteDto unspecificSiteDto = (UnspecificSiteDto) result.data();
                AiExtractorDto aiExtractorDto = aiService.extract(unspecificSiteDto.rawHtml());
                yield new ArticleData(
                        aiExtractorDto.title(),
                        aiExtractorDto.dataCreatedDate(),
                        aiExtractorDto.content(),
                        aiExtractorDto.imageUrl(),
                        aiExtractorDto.sources()
                );
            }
        };
    }
}
