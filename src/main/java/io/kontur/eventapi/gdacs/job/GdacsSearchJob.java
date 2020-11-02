package io.kontur.eventapi.gdacs.job;

import feign.FeignException;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.gdacs.client.GdacsClient;
import io.kontur.eventapi.gdacs.dto.AlertForInsertDataLake;
import io.kontur.eventapi.gdacs.service.GdacsService;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.annotation.Timed;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.kontur.eventapi.util.DateTimeUtil.parseDateTimeFromString;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;

@Component
public class GdacsSearchJob implements Runnable {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsSearchJob.class);

    public static OffsetDateTime XML_PUB_DATE = DateTimeUtil.uniqueOffsetDateTime();

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
    @Timed(value = "job.gdacs.gdacsSearchJob", longTask = true)
    public void run() {
        try {
            LOG.info("Gdacs import job has started");
            String xml = gdacsClient.getXml();
            List<String> links = getLinksAndPubDate(xml);
            List<String> alerts = getAlerts(links);
            List<AlertForInsertDataLake> alertsForDataLake = getAlertsForDataLake(alerts);
            saveAlerts(alertsForDataLake);
            LOG.info("Gdacs import job has finished");
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e) {
            LOG.warn("Gdacs import job has failed", e);
        }
    }

    List<String> getLinksAndPubDate(String xml) throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        var links = new ArrayList<String>();

        var builderFactory = DocumentBuilderFactory.newInstance();
        var builder = builderFactory.newDocumentBuilder();
        var inputStream = new ByteArrayInputStream(xml.getBytes());
        var xmlDocument = builder.parse(inputStream);
        var xPath = XPathFactory.newInstance().newXPath();

        String pathToLinks = "/rss/channel/item/link/text()";
        String pathToPubDate = "/rss/channel/pubDate/text()";

        var linkNodeList = (NodeList) xPath.compile(pathToLinks).evaluate(xmlDocument, XPathConstants.NODESET);
        var pubDateString = (String) xPath.compile(pathToPubDate).evaluate(xmlDocument, XPathConstants.STRING);

        XML_PUB_DATE = parseDateTimeFromString(pubDateString);

        for (int i = 0; i < linkNodeList.getLength(); i++) {
            String link = linkNodeList.item(i).getNodeValue();
            links.add(link.replace("https://www.gdacs.org", ""));
        }
        return links;
    }

    List<String> getAlerts(List<String> links) {
        return links.stream()
                .map(this::getAlertAfterHandleException)
                .filter(not(String::isEmpty))
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

    List<AlertForInsertDataLake> getAlertsForDataLake(List<String> alertXmlList) throws ParserConfigurationException, XPathExpressionException {
        var alertsForDataLake = new ArrayList<AlertForInsertDataLake>();

        var builderFactory = DocumentBuilderFactory.newInstance();
        var builder = builderFactory.newDocumentBuilder();
        var xPath = XPathFactory.newInstance().newXPath();

        String pathToDateModified = "/alert/info/parameter";

        var xPathExpressionToParameters = xPath.compile(pathToDateModified);

        for (String alertXml : alertXmlList) {
            var alertDataLakeOptional = parseAlert(builder, xPathExpressionToParameters, alertXml);
            alertDataLakeOptional.ifPresent(alertsForDataLake::add);
        }
        return alertsForDataLake;
    }

    private Optional<AlertForInsertDataLake> parseAlert(DocumentBuilder builder, XPathExpression xPathExpressionToParameters, String alertXml) {
        try {
            var inputStream = new ByteArrayInputStream(alertXml.getBytes());
            var xmlDocument = builder.parse(inputStream);
            var parameterNodeList = (NodeList) xPathExpressionToParameters.evaluate(xmlDocument, XPathConstants.NODESET);
            String eventId = "";
            String eventType = "";
            String updateDateString = "";

            for (int i = 0; i < parameterNodeList.getLength(); i++) {
                String valueName = parameterNodeList.item(i).getChildNodes().item(1).getTextContent();
                switch (valueName) {
                    case "datemodified":
                        updateDateString = parameterNodeList.item(i).getChildNodes().item(3).getTextContent();
                        break;
                    case "eventid":
                        eventId = parameterNodeList.item(i).getChildNodes().item(3).getTextContent();
                        break;
                    case "eventtype":
                        eventType = parameterNodeList.item(i).getChildNodes().item(3).getTextContent();
                        break;
                }

            }
            if (StringUtils.isEmpty(eventType) || StringUtils.isEmpty(eventId) || StringUtils.isEmpty(updateDateString)) {
                LOG.warn("Alerts xml does not have parameter: {}", alertXml);
                return Optional.empty();
            }

            String externalId = eventType + "_" + eventId;
            return Optional.of(new AlertForInsertDataLake(
                    OffsetDateTime.parse(updateDateString, DateTimeFormatter.RFC_1123_DATE_TIME),
                    externalId,
                    alertXml
            ));

        } catch (IOException | SAXException | XPathExpressionException e) {
            LOG.warn("Alerts xml is not valid and can not be parsed: {}", alertXml);
        } catch (DateTimeParseException e) {
            LOG.warn("Alerts xml value of parameter datemodified can not be parsed: {}", alertXml);
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




