package io.kontur.eventapi.gdacs.job;

import com.google.common.base.Strings;
import feign.FeignException;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.gdacs.client.GdacsClient;
import io.kontur.eventapi.gdacs.dto.AlertForInsertDataLake;
import io.kontur.eventapi.gdacs.service.GdacsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

@Component
public class GdacsSearchJob implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsSearchJob.class);

    private final GdacsClient gdacsClient;
    private final DataLakeDao dataLakeDao;
    private final GdacsService gdacsService;

    @Autowired
    public GdacsSearchJob(GdacsClient gdacsClient, DataLakeDao dataLakeDao, GdacsService gdacsService) {
        this.gdacsClient = gdacsClient;
        this.dataLakeDao = dataLakeDao;
        this.gdacsService = gdacsService;
    }

    @Override
    public void run() {
        try {
            LOG.info("Gdacs import job has started");
            String xml = gdacsClient.getXml();
            List<String> links = getLinks(xml);
            List<String> alerts = getAlerts(links);
            List<AlertForInsertDataLake> alertsForDataLake = getAlertsForDateLake(alerts);
            saveAlerts(alertsForDataLake);
            LOG.info("Gdacs import job has finished");
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e) {
            LOG.warn("Gdacs import job has failed", e);
        }
    }

    List<String> getLinks(String xml) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        var links = new ArrayList<String>();

        var builderFactory = DocumentBuilderFactory.newInstance();
        var builder = builderFactory.newDocumentBuilder();
        var inputStream = new ByteArrayInputStream(xml.getBytes());
        var xmlDocument = builder.parse(inputStream);
        var xPath = XPathFactory.newInstance().newXPath();

        String pathToItems = "/rss/channel/item";

        var itemNodeList = (NodeList) xPath.compile(pathToItems).evaluate(xmlDocument, XPathConstants.NODESET);

        for (int i = 0; i < itemNodeList.getLength(); i++) {
            int indexOfItems = i + 1;
            String pathToLink = "/rss/channel/item[" + indexOfItems + "]/link/text()";
            var link = (String) xPath.compile(pathToLink).evaluate(xmlDocument, XPathConstants.STRING);
            links.add(link.replace("https://www.gdacs.org", ""));
        }
        return links;
    }

    List<String> getAlerts(List<String> links) {
        return links.stream()
                .map(this::getAlertAfterHandleException)
                .filter(not(String::isEmpty))
                .filter(alert -> alert.contains("<alert"))
                .map(alert -> alert.startsWith("\uFEFF") ? alert.substring(1) : alert)
                .collect(toList());
    }

    private String getAlertAfterHandleException(String link) {
        try {
             return gdacsClient.getAlertByLink(link);
        } catch (FeignException e) {
            LOG.warn("Alert by link https://www.gdacs.org{} not found", link);
        }
        return "";
    }

    List<AlertForInsertDataLake> getAlertsForDateLake(List<String> alerts) throws ParserConfigurationException, XPathExpressionException {
        var alertsForDataLake = new ArrayList<AlertForInsertDataLake>();

        var builderFactory = DocumentBuilderFactory.newInstance();
        var builder = builderFactory.newDocumentBuilder();
        var xPath = XPathFactory.newInstance().newXPath();

        String pathToExternalId = "/alert/identifier/text()";
        String pathToDateModified = "/alert/info/parameter";

        var xPathExpressionToExternalId = xPath.compile(pathToExternalId);
        var xPathExpressionToParameters = xPath.compile(pathToDateModified);

        for (String alert : alerts) {
            var alertDataLakeOptional = parseAlert(builder, xPath, xPathExpressionToExternalId, xPathExpressionToParameters, alert);
            alertDataLakeOptional.ifPresent(alertsForDataLake::add);
        }
        return alertsForDataLake;
    }

    private Optional<AlertForInsertDataLake> parseAlert(DocumentBuilder builder, XPath xPath, XPathExpression xPathExpressionToExternalId, XPathExpression xPathExpressionToParameters, String alert) {
        try {
            var inputStream = new ByteArrayInputStream(alert.getBytes());
            var xmlDocument = builder.parse(inputStream);

            var externalId = (String) xPathExpressionToExternalId.evaluate(xmlDocument, XPathConstants.STRING);
            if (Strings.isNullOrEmpty(externalId)) {
                return Optional.empty();
            }

            var parameterNodeList = (NodeList) xPathExpressionToParameters.evaluate(xmlDocument, XPathConstants.NODESET);

            return parseParameters(xPath, alert, xmlDocument, externalId, parameterNodeList);
        } catch (IOException | SAXException | XPathExpressionException | DateTimeParseException e) {
            LOG.warn("Alert is not valid: {}", alert);
            return Optional.empty();
        }
    }

    private Optional<AlertForInsertDataLake> parseParameters(XPath xPath, String alert, Document xmlDocument, String externalId, NodeList parameterNodeList) throws XPathExpressionException {
        for (int i = 0; i < parameterNodeList.getLength(); i++) {
            int indexOfParameters = i + 1;
            String pathToValueName = "/alert/info/parameter[" + indexOfParameters + "]/valueName/text()";
            var valueName = (String) xPath.compile(pathToValueName).evaluate(xmlDocument, XPathConstants.STRING);

            if (valueName.equals("datemodified")) {
                String pathToUpdateDate = "/alert/info/parameter[" + indexOfParameters + "]/value/text()";
                var updateDateString = (String) xPath.compile(pathToUpdateDate).evaluate(xmlDocument, XPathConstants.STRING);

                var updateDateOffset = ZonedDateTime
                        .parse(updateDateString, DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toOffsetDateTime();

                var parsedAlert = new AlertForInsertDataLake(
                        updateDateOffset,
                        externalId,
                        alert
                );
                return Optional.of(parsedAlert);
            }
        }
        return Optional.empty();
    }

    void saveAlerts(List<AlertForInsertDataLake> alerts) {
        alerts.forEach(alert -> {
            var dataLakes = dataLakeDao.getDataLakeByExternalIdAndUpdateDate(
                    alert.getExternalId(),
                    alert.getUpdateDate()
            );
            if (dataLakes.isEmpty()) {
                gdacsService.saveGdacs(alert);
            }
        });
    }
}




