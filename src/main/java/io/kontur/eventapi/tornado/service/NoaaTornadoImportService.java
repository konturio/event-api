package io.kontur.eventapi.tornado.service;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Component
public class NoaaTornadoImportService {
    private final static Logger LOG = LoggerFactory.getLogger(NoaaTornadoImportService.class);

    @Value("${noaaTornado.host}")
    private String URL;

    public Map<String, OffsetDateTime> parseFilenamesAndUpdateDates() {
        try {
            return Jsoup.connect(URL).get()
                    .select("tr:has(a:matches(StormEvents_details))")
                    .stream()
                    .map(element -> {
                        String[] rowItems = StringUtils.split(element.text());
                        String filename = rowItems[0];
                        String updateDate = rowItems[1] + "T" + rowItems[2] + "Z";
                        return Map.entry(filename, OffsetDateTime.parse(updateDate));
                    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    public String decompressGZIP(byte[] gzip) throws IOException {
        GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(gzip));
        return new String(gzipInputStream.readAllBytes());
    }
}
