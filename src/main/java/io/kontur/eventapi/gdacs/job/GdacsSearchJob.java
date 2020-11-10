package io.kontur.eventapi.gdacs.job;

import io.kontur.eventapi.gdacs.converter.GdacsAlertXmlParser;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import io.kontur.eventapi.gdacs.service.GdacsService;
import io.kontur.eventapi.util.DateTimeUtil;
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
public class GdacsSearchJob implements Runnable {

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
    @Timed(value = "job.gdacs.gdacsSearchJob", longTask = true)
    public void run() {
        try {
            LOG.info("Gdacs import job has started");
            var xmlOpt = gdacsService.getGdacsXml();
            if (xmlOpt.isPresent()) {
                String xml = xmlOpt.get();
                setPubDate(xml);
                var links = getLinks(xml);
                var alerts = gdacsService.getAlerts(links);
                var parsedAlerts = getSortedParsedAlerts(alerts);
                var dataLakes = gdacsService.getDataLakes(parsedAlerts);
                gdacsService.saveGdacs(dataLakes);
            }
            LOG.info("Gdacs import job has finished");
        } catch (DateTimeParseException e) {
            LOG.warn("Parsing pubDate from Gdacs was failed");
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e) {
            LOG.warn("Gdacs import job has failed", e);
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




