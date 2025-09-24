package org.tuna.zoopzoop.backend.domain.datasource.crawler.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.service.CrawlerManagerService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.ArticleData;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
public class CrawlerTestController {
    private final CrawlerManagerService crawlerManagerService;

    @GetMapping("/crawl")
    public ArticleData crawl(@RequestParam String url) throws Exception {
        return crawlerManagerService.extractContent(url);
    }
}
