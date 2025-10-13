package org.tuna.zoopzoop.backend.domain.datasource.crawler.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tuna.zoopzoop.backend.domain.datasource.crawler.service.CrawlerManagerService;
import org.tuna.zoopzoop.backend.domain.datasource.dataprocessor.service.DataProcessorService;
import org.tuna.zoopzoop.backend.domain.datasource.dto.DataSourceDto;
import org.tuna.zoopzoop.backend.domain.datasource.repository.TagRepository;

import java.util.ArrayList;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
public class CrawlerTestController {
    private final CrawlerManagerService crawlerManagerService;
    private final DataProcessorService dataProcessorService;
    private final TagRepository tagRepository;

    @GetMapping("/crawl")
    public DataSourceDto crawl(@RequestParam String url) throws Exception {
        return dataProcessorService.process(url, new ArrayList<>());
    }
}
