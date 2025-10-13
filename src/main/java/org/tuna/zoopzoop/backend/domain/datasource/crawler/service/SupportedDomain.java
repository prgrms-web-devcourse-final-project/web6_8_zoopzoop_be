package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

public enum SupportedDomain {
    NAVERNEWS("n.news.naver.com"),
    NAVERBLOG("blog.naver.com"),
    VELOG("velog.io"),
    TISTORY("tistory.com");

    private final String domain;

    SupportedDomain(String domain) {
        this.domain = domain;
    }

    public String getDomain() {
        return domain;
    }
}
