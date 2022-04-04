package io.kontur.eventapi.gdacs.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.gdacs.converter.GdacsAlertXmlParser;
import io.kontur.eventapi.gdacs.service.GdacsService;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Component
public class GdacsSearchJob extends AbstractJob {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsSearchJob.class);

    public static PubDate XML_PUB_DATE = new PubDate(DateTimeUtil.uniqueOffsetDateTime());

    private final GdacsService gdacsService;
    private final GdacsAlertXmlParser gdacsAlertParser;
    private final DataLakeDao dataLakeDao;

    @Autowired
    public GdacsSearchJob(GdacsService gdacsService, GdacsAlertXmlParser gdacsAlertParser, DataLakeDao dataLakeDao,
                          MeterRegistry meterRegistry) {
        super(meterRegistry);
        this.gdacsService = gdacsService;
        this.gdacsAlertParser = gdacsAlertParser;
        this.dataLakeDao = dataLakeDao;
        Gauge.builder("gdacsFeedXML", XML_PUB_DATE, (pub) -> pub.getPubDate().until(DateTimeUtil.uniqueOffsetDateTime(), ChronoUnit.HOURS))
                .description("Gdacs CAP feed did not update (hours)")
                .register(meterRegistry);
    }

    @Override
    public void execute() {
        var xmlOpt = gdacsService.fetchGdacsXml();
        if (xmlOpt.isPresent()) {
            Counter counter = meterRegistry.counter("import.gdacs.counter");
            String xml = xmlOpt.get();
            try {
                setPubDate(xml);
                var alerts = gdacsAlertParser.getAlerts(xml);
                var filteredAlerts = filterExistsAlerts(alerts);
                var parsedAlerts = gdacsAlertParser.getParsedAlertsToGdacsSearchJob(filteredAlerts);
                var dataLakes = gdacsService.createDataLakeListWithAlertsAndGeometry(parsedAlerts, counter);
                gdacsService.saveGdacs(dataLakes);
            } catch (DateTimeParseException e) {
                LOG.error("Parsing pubDate from Gdacs was failed");
            } catch (SAXException | ParserConfigurationException | XPathExpressionException | IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public Map<String, String> filterExistsAlerts(Map<String, String> alerts) {
        Map<String, String> filteredAlerts = new HashMap<>();
        Map<String, DataLake> existsDataLakes = new HashMap<>();
        dataLakeDao.getDataLakesByExternalIds(alerts.keySet())
                .forEach(dataLake -> existsDataLakes.put(dataLake.getExternalId(), dataLake));
        alerts.keySet().stream()
                .filter(key -> !existsDataLakes.containsKey(key))
                .forEach(key -> filteredAlerts.put(key, alerts.get(key)));
        return filteredAlerts;
    }

    @Override
    public String getName() {
        return "gdacsSearch";
    }

    private void setPubDate(String xml) throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        XML_PUB_DATE.setPubDate(gdacsAlertParser.getPubDate(xml));
    }

    @Data @AllArgsConstructor
    private static class PubDate {
        private OffsetDateTime pubDate;
    }
}




