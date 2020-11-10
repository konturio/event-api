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
import java.util.Comparator;
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
            List<AlertForInsertDataLake> alertsForDataLake = getSortedBySentAlertsForDataLake(alerts);
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

    List<AlertForInsertDataLake> getSortedBySentAlertsForDataLake(List<String> alertXmlList) throws ParserConfigurationException, XPathExpressionException {
        var alertsForDataLake = new ArrayList<AlertForInsertDataLake>();

        var builderFactory = DocumentBuilderFactory.newInstance();
        var builder = builderFactory.newDocumentBuilder();
        var xPath = XPathFactory.newInstance().newXPath();

        String pathToExternalId = "/alert/identifier/text()";
        String pathToDateModified = "/alert/info/parameter";
        String pathToSent = "/alert/sent/text()";

        var externalIdExpression = xPath.compile(pathToExternalId);
        var xPathExpressionToParameters = xPath.compile(pathToDateModified);
        var xPathExpressionToSent = xPath.compile(pathToSent);

        for (String alertXml : alertXmlList) {
            var alertDataLakeOptional = parseAlert(builder, externalIdExpression, xPathExpressionToParameters, xPathExpressionToSent, alertXml);
            alertDataLakeOptional.ifPresent(alertsForDataLake::add);
        }
        alertsForDataLake.sort(Comparator.comparing(AlertForInsertDataLake::getSentDateTime));
        return alertsForDataLake;
    }

    private Optional<AlertForInsertDataLake> parseAlert(DocumentBuilder builder,
                                                        XPathExpression externalIdExpression,
                                                        XPathExpression xPathExpressionToParameters,
                                                        XPathExpression xPathExpressionToSent, String alertXml) {
        try {
            var inputStream = new ByteArrayInputStream(alertXml.getBytes());
            var xmlDocument = builder.parse(inputStream);
            var externalId = (String) externalIdExpression.evaluate(xmlDocument, XPathConstants.STRING);
            var parameterNodeList = (NodeList) xPathExpressionToParameters.evaluate(xmlDocument, XPathConstants.NODESET);
            var sentDateTime = (String) xPathExpressionToSent.evaluate(xmlDocument, XPathConstants.STRING);
            String eventId = "";
            String eventType = "";
            String updateDateString = "";

            for (int i = 0; i < parameterNodeList.getLength(); i++) {
                String valueName = getValueNameByParameterName(parameterNodeList, i);
                switch (valueName) {
                    case "datemodified":
                        updateDateString = getValueByParameterName(parameterNodeList, i);
                        break;
                    case "eventid":
                        eventId = getValueByParameterName(parameterNodeList, i);
                        break;
                    case "eventtype":
                        eventType = getValueByParameterName(parameterNodeList, i);
                        break;
                }

            }
            if (StringUtils.isEmpty(eventType) || StringUtils.isEmpty(eventId) || StringUtils.isEmpty(updateDateString)) {
                LOG.warn("Alerts xml does not have parameter: {}", alertXml);
                return Optional.empty();
            }

            return Optional.of(new AlertForInsertDataLake(
                    OffsetDateTime.parse(updateDateString, DateTimeFormatter.RFC_1123_DATE_TIME),
                    externalId,
                    alertXml,
                    OffsetDateTime.parse(sentDateTime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            ));

        } catch (IOException | SAXException | XPathExpressionException e) {
            LOG.warn("Alerts xml is not valid and can not be parsed: {}", alertXml);
        } catch (DateTimeParseException e) {
            LOG.warn("Alerts xml value of parameter datemodified or sent can not be parsed: {}", alertXml);
        }
        return Optional.empty();
    }

    private String getValueNameByParameterName(NodeList parameterNodeList, int index){
        var childNodes = parameterNodeList.item(index).getChildNodes();
        for(int i = 0; i < childNodes.getLength(); i++){
            var node = childNodes.item(i);
            if(node.getNodeName().equals("valueName")){
                return node.getTextContent();
            }
        }
        return "";
    }

    private String getValueByParameterName(NodeList parameterNodeList, int index){
        var childNodes = parameterNodeList.item(index).getChildNodes();
        for(int i = 0; i < childNodes.getLength(); i++){
            var node = childNodes.item(i);
            if(node.getNodeName().equals("value")){
                return node.getTextContent();
            }
        }
        return "";
    }

    void saveAlerts(List<AlertForInsertDataLake> alerts) {
        alerts.forEach(alert -> {
            var dataLakes = dataLakeDao.getDataLakesByExternalId(alert.getExternalId());
            if (dataLakes.isEmpty()) {
                gdacsService.saveGdacs(alert);
            }
        });
    }
}




