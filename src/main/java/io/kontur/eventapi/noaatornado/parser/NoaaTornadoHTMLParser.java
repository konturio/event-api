package io.kontur.eventapi.noaatornado.parser;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class NoaaTornadoHTMLParser {

    private final static Logger LOG = LoggerFactory.getLogger(NoaaTornadoHTMLParser.class);

    @Value("${noaaTornado.host}")
    private String URL;

    public Map<String, OffsetDateTime> parseFilenamesAndUpdateDates() {
        try {
            Document doc = Jsoup.connect(URL).get();
            Elements tableRows = doc.select("tr:has(a:matches(StormEvents_details))");
            return tableRows.stream()
                    .map(row -> {
                        String[] rowItems = StringUtils.split(row.text());
                        String filename = rowItems[0];
                        String updateDate = rowItems[1] + "T" + rowItems[2] + "Z";
                        return Map.entry(filename, OffsetDateTime.parse(updateDate));
                    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyMap();
    }
}
