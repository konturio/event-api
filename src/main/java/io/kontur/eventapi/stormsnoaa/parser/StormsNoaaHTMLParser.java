package io.kontur.eventapi.stormsnoaa.parser;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class StormsNoaaHTMLParser {

    private final static Logger LOG = LoggerFactory.getLogger(StormsNoaaHTMLParser.class);

    @Value("${stormsNoaa.host}")
    private String URL;

    public List<FileInfo> parseFilesInfo() {
        try {
            Document doc = Jsoup.connect(URL).get();
            Elements tableRows = doc.select("tr:has(a:matches(StormEvents_details))");
            return tableRows.stream().map(
                    row -> {
                        String[] rowItems = StringUtils.split(row.text());
                        String filename = rowItems[0];
                        OffsetDateTime updatedAt = OffsetDateTime.parse(rowItems[1] + "T" + rowItems[2] + "Z");
                        return new FileInfo(filename, updatedAt);
                    }).collect(Collectors.toList());
        } catch (Exception e) {
            LOG.warn(e.getMessage());
        }
        return Collections.emptyList();
    }
}
