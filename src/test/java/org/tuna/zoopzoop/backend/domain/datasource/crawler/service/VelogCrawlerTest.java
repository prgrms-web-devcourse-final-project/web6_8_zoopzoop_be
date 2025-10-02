package org.tuna.zoopzoop.backend.domain.datasource.crawler.service;

class VelogCrawlerTest {

    private final VelogCrawler velogCrawler = new VelogCrawler();

    // 날짜 바뀐 velog 포스트에 대해 에러 처리 필요
    // Text '어제' could not be parsed at index 0
//    java.time.format.DateTimeParseException
//    @Test
//    void testExtract() throws IOException {
//        Document doc = Jsoup.connect("https://velog.io/@hyeonnnnn/VampireSurvivorsClone-04.-PoolManager").get();
//        CrawlerResult<?> result = velogCrawler.extract(doc);
//        assertThat(result).isNotNull();
//
//        System.out.println(result);
//    }
}