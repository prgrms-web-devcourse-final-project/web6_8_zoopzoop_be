package org.tuna.zoopzoop.backend.domain.news.dto.res;

import java.util.List;

public record ResBodyForNaverNews(
        int total,
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
            String noTags = text.replaceAll("<.*?>", "");
            return noTags.replaceAll("&[a-zA-Z0-9#]+;", "");
        }
    }
}
