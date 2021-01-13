package io.kontur.eventapi.gdacs.job;

import io.kontur.eventapi.gdacs.converter.GdacsAlertXmlParser;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import io.kontur.eventapi.gdacs.service.GdacsService;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
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
import java.util.Comparator;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class GdacsSearchJob extends AbstractJob {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsSearchJob.class);

    public static OffsetDateTime XML_PUB_DATE = DateTimeUtil.uniqueOffsetDateTime();

    private final GdacsService gdacsService;
    private final GdacsAlertXmlParser gdacsAlertParser;

    @Autowired
    public GdacsSearchJob(GdacsService gdacsService, GdacsAlertXmlParser gdacsAlertParser) {
        this.gdacsService = gdacsService;
        this.gdacsAlertParser = gdacsAlertParser;
    }

    @Override
    @Counted(value = "job.gdacs_search.counter")
    @Timed(value = "job.gdacs_search.in_progress_timer", longTask = true)
    public void execute() throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        try {
            var xmlOpt = gdacsService.fetchGdacsXml();
            if (xmlOpt.isPresent()) {
                String xml = xmlOpt.get();
                setPubDate(xml);
                var links = getLinks(xml);
                var alerts = gdacsService.fetchAlerts(links);
                var parsedAlerts = getSortedParsedAlerts(alerts);
                var dataLakes = gdacsService.createDataLakeListWithAlertsAndGeometry(parsedAlerts);
                gdacsService.saveGdacs(dataLakes);
            }
        } catch (DateTimeParseException e) {
            LOG.warn("Parsing pubDate from Gdacs was failed");
            throw e;
        }
    }

    void setPubDate(String xml) throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        XML_PUB_DATE = gdacsAlertParser.getPubDate(xml);
    }

    List<String> getLinks(String xml) throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        return gdacsAlertParser.getLinks(xml).stream()
                .map(link -> link.replace("https://www.gdacs.org", ""))
                .collect(toList());
    }

    List<ParsedAlert> getSortedParsedAlerts(List<String> alerts) throws XPathExpressionException, ParserConfigurationException {
        return gdacsAlertParser.getParsedAlertsToGdacsSearchJob(alerts).stream()
                .sorted(Comparator.comparing(ParsedAlert::getSent))
                .collect(toList());
    }
}




