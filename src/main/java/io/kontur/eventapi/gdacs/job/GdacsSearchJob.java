package io.kontur.eventapi.gdacs.job;

import io.kontur.eventapi.gdacs.converter.GdacsAlertXmlParser;
import io.kontur.eventapi.gdacs.service.GdacsService;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
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

@Component
public class GdacsSearchJob extends AbstractJob {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsSearchJob.class);

    public static OffsetDateTime XML_PUB_DATE = DateTimeUtil.uniqueOffsetDateTime();

    private final GdacsService gdacsService;
    private final GdacsAlertXmlParser gdacsAlertParser;

    @Autowired
    public GdacsSearchJob(GdacsService gdacsService, GdacsAlertXmlParser gdacsAlertParser, MeterRegistry meterRegistry) {
        super(meterRegistry);
        this.gdacsService = gdacsService;
        this.gdacsAlertParser = gdacsAlertParser;
        Gauge.builder("gdacsFeedXML", XML_PUB_DATE, (x) -> XML_PUB_DATE.until(OffsetDateTime.now(), ChronoUnit.HOURS))
                .description("Gdacs CAP feed did not update (hours). Last pubDate: " + XML_PUB_DATE)
                .register(meterRegistry);
    }

    @Override
    public void execute() {
        var xmlOpt = gdacsService.fetchGdacsXml();
        if (xmlOpt.isPresent()) {
            String xml = xmlOpt.get();
            try {
                setPubDate(xml);
                var alerts = gdacsAlertParser.getAlerts(xml);
                var parsedAlerts = gdacsAlertParser.getParsedAlertsToGdacsSearchJob(alerts);
                var dataLakes = gdacsService.createDataLakeListWithAlertsAndGeometry(parsedAlerts);
                gdacsService.saveGdacs(dataLakes);
            } catch (DateTimeParseException e) {
                LOG.error("Parsing pubDate from Gdacs was failed");
            } catch (SAXException | ParserConfigurationException | XPathExpressionException | IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public String getName() {
        return "gdacsSearch";
    }

    private void setPubDate(String xml) throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        XML_PUB_DATE = gdacsAlertParser.getPubDate(xml);
    }
}




