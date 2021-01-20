package io.kontur.eventapi.gdacs.converter;

import feign.FeignException;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import io.kontur.eventapi.gdacs.job.GdacsSearchJob;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.kontur.eventapi.gdacs.service.GdacsService.ALERT_BY_LINK;
import static io.kontur.eventapi.util.DateTimeUtil.parseDateTimeFromString;

@Component
public class GdacsAlertXmlParser {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsSearchJob.class);

    public OffsetDateTime getPubDate(String xml) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        var xmlDocument = getXmlDocument(xml);
        var xPath = XPathFactory.newInstance().newXPath();
        String pathToPubDate = "/rss/channel/pubDate/text()";
        var pubDateString = (String) xPath.compile(pathToPubDate).evaluate(xmlDocument, XPathConstants.STRING);
        return parseDateTimeFromString(pubDateString);
    }

    public List<String> getLinks(String xml) throws XPathExpressionException, IOException, SAXException, ParserConfigurationException {
        var links = new ArrayList<String>();

        var xmlDocument = getXmlDocument(xml);
        var xPath = XPathFactory.newInstance().newXPath();
        String pathToLinks = "/rss/channel/item/link/text()";

        var linkNodeList = (NodeList) xPath.compile(pathToLinks).evaluate(xmlDocument, XPathConstants.NODESET);

        for (int i = 0; i < linkNodeList.getLength(); i++) {
            links.add(linkNodeList.item(i).getNodeValue());
        }
        return links;
    }

    public List<ParsedAlert> getParsedAlertsToGdacsSearchJob(Map<String, String> alertsByLinks) throws ParserConfigurationException, XPathExpressionException {
        var parsedAlerts = new ArrayList<ParsedAlert>();

        var xPath = XPathFactory.newInstance().newXPath();

        String pathToExternalId = "/alert/identifier/text()";
        String pathToDateModified = "/alert/info/parameter";
        String pathToSent = "/alert/sent/text()";

        var externalIdExpression = xPath.compile(pathToExternalId);
        var xPathExpressionToParameters = xPath.compile(pathToDateModified);
        var xPathExpressionToSent = xPath.compile(pathToSent);

        for (Map.Entry<String, String> alertXml : alertsByLinks.entrySet()) {
            parseAlert(externalIdExpression, xPathExpressionToParameters,
                    xPathExpressionToSent, alertXml.getKey(), alertXml.getValue()).ifPresent(parsedAlerts::add);
        }

        return parsedAlerts;
    }

    private Optional<ParsedAlert> parseAlert(XPathExpression externalIdExpression, XPathExpression xPathExpressionToParameters,
                                             XPathExpression xPathExpressionToSent, String link, String alertXml) throws ParserConfigurationException {
        String externalId = "";
        try {

            var xmlDocument = getXmlDocument(alertXml);
            var parameterNodeList = (NodeList) xPathExpressionToParameters.evaluate(xmlDocument, XPathConstants.NODESET);
            var sentDateTimeString = (String) xPathExpressionToSent.evaluate(xmlDocument, XPathConstants.STRING);
            externalId = (String) externalIdExpression.evaluate(xmlDocument, XPathConstants.STRING);

            String eventId = "";
            String eventType = "";
            String dateModified = "";
            String currentEpisodeId = "";

            for (int i = 0; i < parameterNodeList.getLength(); i++) {
                String valueName = getValueNameByParameterName(parameterNodeList, i);
                switch (valueName) {
                    case "datemodified":
                        dateModified = getValueByParameterName(parameterNodeList, i);
                        break;
                    case "eventid":
                        eventId = getValueByParameterName(parameterNodeList, i);
                        break;
                    case "eventtype":
                        eventType = getValueByParameterName(parameterNodeList, i);
                        break;
                    case "currentepisodeid":
                        currentEpisodeId = getValueByParameterName(parameterNodeList, i);
                        break;
                }
            }
            if (StringUtils.isEmpty(eventType) || StringUtils.isEmpty(eventId) || StringUtils.isEmpty(dateModified)) {
                LOG.warn(ALERT_BY_LINK + " does not have parameter: {}", link, alertXml);
                return Optional.empty();
            }

            return Optional.of(new ParsedAlert(
                    OffsetDateTime.parse(dateModified, DateTimeFormatter.RFC_1123_DATE_TIME),
                    OffsetDateTime.parse(sentDateTimeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    externalId,
                    eventId,
                    eventType,
                    currentEpisodeId,
                    alertXml));

        } catch (IOException | SAXException | XPathExpressionException e) {
            LOG.warn(ALERT_BY_LINK + " is not valid and can not be parsed: {}", link, alertXml);
        } catch (DateTimeParseException e) {
            LOG.warn(ALERT_BY_LINK + " value of parameter datemodified or sent can not be parsed: {}", link, externalId);
        }
        return Optional.empty();
    }

    public ParsedAlert getParsedAlertToNormalization(String xml) throws ParserConfigurationException,
            IOException, SAXException, XPathExpressionException, FeignException {

        var xmlDocument = getXmlDocument(xml);
        var xPath = XPathFactory.newInstance().newXPath();

        String pathToEvent = "/alert/info/event/text()";
        String pathToHeadline = "/alert/info/headline/text()";
        String pathToSeverity = "/alert/info/severity/text()";
        String pathToDescription = "/alert/info/description/text()";
        String pathToParameters = "/alert/info/parameter";

        var event = (String) xPath.compile(pathToEvent).evaluate(xmlDocument, XPathConstants.STRING);
        var headline = (String) xPath.compile(pathToHeadline).evaluate(xmlDocument, XPathConstants.STRING);
        var severity = (String) xPath.compile(pathToSeverity).evaluate(xmlDocument, XPathConstants.STRING);
        var description = (String) xPath.compile(pathToDescription).evaluate(xmlDocument, XPathConstants.STRING);

        var parsedAlert = new ParsedAlert();

        parsedAlert.setHeadLine(headline);
        parsedAlert.setDescription(description);
        parsedAlert.setEvent(event);
        parsedAlert.setSeverity(severity);

        var parameterNodeList = (NodeList) xPath.compile(pathToParameters)
                .evaluate(xmlDocument, XPathConstants.NODESET);
        setDataFromParameters(parsedAlert, parameterNodeList);
        return parsedAlert;
    }

    private void setDataFromParameters(ParsedAlert parsedAlert, NodeList parameterNodeList) throws DateTimeParseException {

        for (int i = 0; i < parameterNodeList.getLength(); i++) {
            String valueName = getValueNameByParameterName(parameterNodeList, i);
            switch (valueName) {
                case "eventid":
                    parsedAlert.setEventId(getValueByParameterName(parameterNodeList, i));
                    break;
                case "eventtype":
                    parsedAlert.setEventType(getValueByParameterName(parameterNodeList, i));
                    break;
                case "currentepisodeid":
                    parsedAlert.setCurrentEpisodeId(getValueByParameterName(parameterNodeList, i));
                    break;
                case "fromdate":
                    parsedAlert.setFromDate(parseDateTimeFromString(getValueByParameterName(parameterNodeList, i)));
                    break;
                case "todate":
                    parsedAlert.setToDate(parseDateTimeFromString(getValueByParameterName(parameterNodeList, i)));
                    break;
            }
        }
    }

    private Document getXmlDocument(String xml) throws ParserConfigurationException, IOException, SAXException {
        var builderFactory = DocumentBuilderFactory.newInstance();
        var builder = builderFactory.newDocumentBuilder();
        var inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        return builder.parse(inputStream);
    }

    private String getValueNameByParameterName(NodeList parameterNodeList, int index) {
        var childNodes = parameterNodeList.item(index).getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            var node = childNodes.item(i);
            if (node.getNodeName().equals("valueName")) {
                return node.getTextContent();
            }
        }
        return "";
    }

    private String getValueByParameterName(NodeList parameterNodeList, int index) {
        var childNodes = parameterNodeList.item(index).getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            var node = childNodes.item(i);
            if (node.getNodeName().equals("value")) {
                return node.getTextContent();
            }
        }
        return "";
    }
}
