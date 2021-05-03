package io.kontur.eventapi.tornadojapanma.parser;


import io.kontur.eventapi.tornadojapanma.dto.ParsedCase;
import org.apache.commons.lang3.RegExUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.Set;

public abstract class HtmlParser {
    protected final static Logger LOG = LoggerFactory.getLogger(HtmlParser.class);

    @Value("${tornadoJapanMa.host}")
    protected String BASE_URL;

    protected String parseUpdatedAt(int btnNumber) throws IOException {
        Document doc = getDocument(BASE_URL);
        String updatedAtString = doc.select(String.format(".layout tr:nth-child(%d) td:nth-child(3)", btnNumber)).text();
        return RegExUtils.removePattern(updatedAtString, "[^0-9\\.]");
    }

    protected Document getDocument(String url) throws IOException {
        try {
            return Jsoup.connect(url).get();
        } catch (Exception e) {
            LOG.warn("Couldn't fetch URL: " + url);
            try {
                Thread.sleep(10000);
            } catch (InterruptedException interruptedException) {
                LOG.warn(e.getMessage());
            }
            return Jsoup.connect(url).get();
        }
    }

    public abstract String parseUpdatedAt() throws IOException;

    public abstract Set<ParsedCase> parseCases();
}
