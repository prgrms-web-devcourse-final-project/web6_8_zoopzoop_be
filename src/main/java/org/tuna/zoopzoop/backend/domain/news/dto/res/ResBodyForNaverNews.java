package org.tuna.zoopzoop.backend.domain.news.dto.res;

import java.util.List;

public record ResBodyForNaverNews(
        String lastBuildDate,
        int total,
        int start,
        int display,
        List<NewsItem> items
) {
    public record NewsItem(
            String title,
            String link,
            String description,
            String pubDate
    ) {
        public NewsItem(String title, String link, String description, String pubDate) {
            this.title = cleanText(title);
            this.link = link;
            this.description = cleanText(description);
            this.pubDate = pubDate;
        }

        private static String cleanText(String text) {
            if (text == null) return null;
            return text.replaceAll("<.*?>", "");
        }
    }
}
