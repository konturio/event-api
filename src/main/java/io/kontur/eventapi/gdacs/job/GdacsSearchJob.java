package io.kontur.eventapi.gdacs.job;

import feign.FeignException;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.gdacs.client.GdacsClient;
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
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Component
public class GdacsSearchJob implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsSearchJob.class);

    public static OffsetDateTime XML_PUB_DATE = DateTimeUtil.uniqueOffsetDateTime();

    private final GdacsClient gdacsClient;
    private final DataLakeDao dataLakeDao;
    private final GdacsService gdacsService;
    private final GdacsAlertXmlParser gdacsAlertParser;

    @Autowired
    public GdacsSearchJob(GdacsClient gdacsClient, DataLakeDao dataLakeDao, GdacsService gdacsService, GdacsAlertXmlParser gdacsAlertParser) {
        this.gdacsClient = gdacsClient;
        this.dataLakeDao = dataLakeDao;
        this.gdacsService = gdacsService;
        this.gdacsAlertParser = gdacsAlertParser;
    }

    @Override
    @Timed(value = "job.gdacs.gdacsSearchJob", longTask = true)
    public void run() {
        try {
            LOG.info("Gdacs import job has started");
            var xml = gdacsClient.getXml();
            setPubDate(xml);
            var links = getLinks(xml);
            var alerts = getAlerts(links);
            var parsedAlerts = getSortedParsedAlerts(alerts);
            saveAlerts(parsedAlerts);
            LOG.info("Gdacs import job has finished");
        } catch (DateTimeParseException e) {
            LOG.warn("Parsing pubDate from Gdacs was failed");
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e) {
            LOG.warn("Gdacs import job has failed", e);
        }
    }

    private void setPubDate(String xml) throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        XML_PUB_DATE = gdacsAlertParser.getPubDate(xml);
    }

    private List<String> getLinks(String xml) throws SAXException, ParserConfigurationException, XPathExpressionException, IOException {
        return gdacsAlertParser.getLinks(xml).stream()
                .map(link -> link.replace("https://www.gdacs.org", ""))
                .collect(toList());
    }

    private List<String> getAlerts(List<String> links) {
        return links.stream()
                .map(this::getAlertAfterHandleException)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(alert -> alert.startsWith("\uFEFF") ? alert.substring(1) : alert)
                .collect(toList());
    }

    private Optional<String> getAlertAfterHandleException(String link) {
        try {
            return Optional.of(gdacsClient.getAlertByLink(link));
        } catch (FeignException e) {
            LOG.warn("Alert by link https://www.gdacs.org{} not found", link);
        }
        return Optional.empty();
    }

    private List<ParsedAlert> getSortedParsedAlerts(List<String> alerts) throws XPathExpressionException, ParserConfigurationException {
        return gdacsAlertParser.getParsedAlertsToGdacsSearchJob(alerts).stream()
                .sorted(Comparator.comparing(ParsedAlert::getSent))
                .collect(toList());
    }

    private void saveAlerts(List<ParsedAlert> alerts) {
        for (ParsedAlert alert : alerts) {
            var dataLakes = dataLakeDao.getDataLakesByExternalId(alert.getIdentifier());
            if (dataLakes.isEmpty()) {
                var geometry = getGeometryToAlert(
                        alert.getEventType(),
                        alert.getEventId(),
                        alert.getCurrentEpisodeId(),
                        alert.getIdentifier());

                if (geometry.isPresent()) {
                    gdacsService.saveGdacs(alert);
                    gdacsService.saveGdacsGeometry(alert, geometry.get());
                }
            }
        }
    }

    private Optional<String> getGeometryToAlert(String eventType, String eventId, String currentEpisodeId, String externalId) {
        try {
            return Optional.of(
                    gdacsClient.getGeometryByLink(eventType, eventId, currentEpisodeId)
            );
        } catch (FeignException e) {
            LOG.warn("Geometry for gdacs alert has not found. identifier = {}", externalId);
        }
        return Optional.empty();
    }
}




