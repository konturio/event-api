package io.kontur.eventapi.jtwc.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.jtwc.service.JtwcService;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.codec.digest.DigestUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JtwcImportJob extends AbstractJob {

    public static final String JTWC_PROVIDER = "cyclones.jtwc";
    private static final Logger LOG = LoggerFactory.getLogger(JtwcImportJob.class);

    private final JtwcService service;
    private final DataLakeDao dataLakeDao;

    public JtwcImportJob(MeterRegistry registry, JtwcService service, DataLakeDao dataLakeDao) {
        super(registry);
        this.service = service;
        this.dataLakeDao = dataLakeDao;
    }

    @Override
    public String getName() {
        return "jtwcImport";
    }

    @Override
    public void execute() {
        Optional<String> feedOpt = service.fetchFeed();
        if (feedOpt.isEmpty()) {
            return;
        }
        String feed = feedOpt.get();
        Document doc = Jsoup.parse(feed, "", Parser.xmlParser());
        Elements items = doc.select("item");
        List<DataLake> dataLakes = new ArrayList<>();
        for (Element item : items) {
            String title = item.selectFirst("title").text();
            if (isApplicableTitle(title)) {
                String description = item.selectFirst("description").text();
                if (!description.contains("No Current Tropical Cyclone Warnings.")) {
                    String pubDateStr = item.selectFirst("pubDate").text();
                    OffsetDateTime updatedAt = DateTimeUtil.parseDateTimeByPattern(pubDateStr, "EEE, dd MMM yy HH:mm:ss Z");
                    Document descDoc = Jsoup.parse(description);
                    Element link = descDoc.selectFirst("li a:contains(TC Warning Text)");
                    if (link != null) {
                        String href = link.attr("href");
                        String fileName = href.substring(href.lastIndexOf('/') + 1);
                        Optional<String> textOpt = service.fetchProduct(fileName);
                        if (textOpt.isPresent()) {
                            String data = textOpt.get();
                            String externalId = DigestUtils.md5Hex(data);
                            if (dataLakeDao.isNewEvent(externalId, JTWC_PROVIDER, updatedAt.format(DateTimeFormatter.ISO_INSTANT))) {
                                DataLake dataLake = new DataLake(UUID.randomUUID(), externalId, updatedAt, DateTimeUtil.uniqueOffsetDateTime());
                                dataLake.setProvider(JTWC_PROVIDER);
                                dataLake.setData(data);
                                dataLakes.add(dataLake);
                            }
                        }
                    }
                }
            }
        }
        if (!dataLakes.isEmpty()) {
            dataLakeDao.storeDataLakes(dataLakes);
        }
    }

    private boolean isApplicableTitle(String title) {
        return "Current Northwest Pacific/North Indian Ocean* Tropical Systems".equals(title)
                || "Current Central/Eastern Pacific Tropical Systems".equals(title)
                || "Current Southern Hemisphere Tropical Systems".equals(title);
    }
}
