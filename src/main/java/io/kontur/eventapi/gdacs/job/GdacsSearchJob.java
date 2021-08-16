package io.kontur.eventapi.gdacs.job;

import io.kontur.eventapi.gdacs.converter.GdacsAlertXmlParser;
import io.kontur.eventapi.gdacs.service.GdacsService;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.util.DateTimeUtil;
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
                LOG.error(xml);
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




