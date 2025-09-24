package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

public enum SupportedDomain {
    NAVERNEWS("n.news.naver.com");

    private final String domain;

    SupportedDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }
}
