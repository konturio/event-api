package io.kontur.eventapi.jtwc.parser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class JtwcRssParser {

    private static final Logger LOG = LoggerFactory.getLogger(JtwcRssParser.class);

    @Value("${jtwc.rss}")
    private String rssUrl;

    private static final DateTimeFormatter PUBDATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yy HH:mm:ss xx", Locale.ENGLISH);

    private static final List<String> VALID_TITLES = List.of(
            "Current Northwest Pacific/North Indian Ocean* Tropical Systems",
            "Current Central/Eastern Pacific Tropical Systems",
            "Current Southern Hemisphere Tropical Systems"
    );

    public List<RssItem> parse() {
        List<RssItem> result = new ArrayList<>();
        try {
            Document doc = Jsoup.connect(rssUrl).get();
            Elements items = doc.select("item");
            for (Element item : items) {
                String title = item.selectFirst("title").text();
                if (!VALID_TITLES.contains(title)) {
                    continue;
                }
                String description = item.selectFirst("description").text();
                if (description.contains("No Current Tropical Cyclone Warnings.")) {
                    continue;
                }
                String pubDateString = item.selectFirst("pubDate").text();
                OffsetDateTime pubDate = OffsetDateTime.parse(pubDateString, PUBDATE_FORMATTER);
                Document descDoc = Jsoup.parse(description);
                Element link = descDoc.selectFirst("li > a:contains(TC Warning Text)");
                if (link != null) {
                    String href = link.attr("href");
                    result.add(new RssItem(href, pubDate));
                }
            }
        } catch (IOException e) {
            LOG.warn("Failed to fetch JTWC RSS feed: {}", e.getMessage());
        }
        return result;
    }

    public String loadText(String url) throws IOException {
        return Jsoup.connect(url).ignoreContentType(true).execute().body();
    }

    public record RssItem(String link, OffsetDateTime pubDate) {}
}
