package io.kontur.eventapi.gdacs.converter;

import feign.FeignException;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import io.kontur.eventapi.gdacs.job.GdacsSearchJob;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

import static io.kontur.eventapi.util.DateTimeUtil.parseDateTimeFromString;

@Component
public class GdacsAlertXmlParser {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsSearchJob.class);

    private static final String DATE_MODIFIED = "datemodified";
    private static final String EVENT_ID = "eventid";
    private static final String EVENT_TYPE = "eventtype";
    private static final String CURRENT_EPISODE_ID = "currentepisodeid";
    private static final String FROM_DATE = "fromdate";
    private static final String TO_DATE = "todate";
    private static final String LINK = "link";
    private static final String EVENT = "event";
    private static final String HEADLINE = "headline";
    private static final String SEVERITY = "severity";
    private static final String DESCRIPTION = "description";
    private static final String PARAMETER = "parameter";
    private static final String IDENTIFIER = "identifier";
    private static final String ALERT = "alert";
    private static final String VALUE = "value";
    private static final String VALUE_NAME = "valueName";

    private static final String NS = "*";

    public OffsetDateTime getPubDate(String xml) throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        var xmlDocument = getXmlDocument(xml);
        var xPath = XPathFactory.newInstance().newXPath();
        String pathToPubDate = "/rss/channel/pubDate/text()";
        var pubDateString = (String) xPath.compile(pathToPubDate).evaluate(xmlDocument, XPathConstants.STRING);
        return parseDateTimeFromString(pubDateString);
    }

    public List<String> getAlerts(String xml) throws IOException, SAXException, ParserConfigurationException {
        List<String> alerts = new ArrayList<>();
        Document xmlDocument = getXmlDocument(xml);
        NodeList nodeList = xmlDocument.getElementsByTagNameNS(NS, ALERT);
        for (int i = 0; i < nodeList.getLength(); i++) {
            try {
                StringWriter writer = new StringWriter();

                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.transform(new DOMSource(nodeList.item(i)), new StreamResult(writer));

                alerts.add(writer.toString());
            } catch (TransformerException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        return alerts;
    }

    public List<ParsedAlert> getParsedAlertsToGdacsSearchJob(List<String> alertsXml) throws ParserConfigurationException {
        List<ParsedAlert> parsedAlerts = new ArrayList<>();
        for (String alertXml : alertsXml) {
            parseAlert(alertXml).ifPresent(parsedAlerts::add);
        }
        return parsedAlerts;
    }

    private Optional<ParsedAlert> parseAlert(String alertXml) throws ParserConfigurationException {
        try {
            Document xmlDocument = getXmlDocument(alertXml);

            String externalId = getValueByTagName(alertXml, xmlDocument, IDENTIFIER);
            if (StringUtils.isEmpty(externalId)) {
                LOG.warn("Alert does not have identifier: \n" +  alertXml);
                return Optional.empty();
            }

            NodeList parameterNodeList = xmlDocument.getElementsByTagNameNS(NS, PARAMETER);
            Set<String> parameterNames = Set.of(DATE_MODIFIED, EVENT_ID, EVENT_TYPE, CURRENT_EPISODE_ID);
            Map<String, String> parameters = parseParameters(parameterNodeList, parameterNames);

            if (parameters.values().stream().anyMatch(StringUtils::isEmpty)) {
                LOG.warn("Alert does not have parameter: \n" +  alertXml);
                return Optional.empty();
            }

            return Optional.of(new ParsedAlert(
                    OffsetDateTime.parse(parameters.get(DATE_MODIFIED), DateTimeFormatter.RFC_1123_DATE_TIME),
                    externalId,
                    parameters.get(EVENT_ID),
                    parameters.get(EVENT_TYPE),
                    parameters.get(CURRENT_EPISODE_ID),
                    alertXml));

        } catch (IOException | SAXException e) {
            LOG.warn("Alert is not valid and can not be parsed: \n" + alertXml, e);
        } catch (DateTimeParseException e) {
            LOG.warn("Alert value of parameter 'datemodified' can not be parsed: \n" + alertXml, e);
        }
        return Optional.empty();
    }

    public ParsedAlert getParsedAlertToNormalization(String xml) throws ParserConfigurationException,
            IOException, SAXException, XPathExpressionException, FeignException {

        Document xmlDocument = getXmlDocument(xml);
        ParsedAlert parsedAlert = new ParsedAlert();

        parsedAlert.setHeadLine(getValueByTagName(xml, xmlDocument, HEADLINE));
        parsedAlert.setDescription(getValueByTagName(xml, xmlDocument, DESCRIPTION));
        parsedAlert.setEvent(getValueByTagName(xml, xmlDocument, EVENT));
        parsedAlert.setSeverity(getValueByTagName(xml, xmlDocument, SEVERITY));

        NodeList parameterNodeList = xmlDocument.getElementsByTagNameNS(NS, PARAMETER);
        Set<String> parameterNames = Set.of(EVENT_ID, EVENT_TYPE, CURRENT_EPISODE_ID, FROM_DATE, TO_DATE, LINK);
        Map<String, String> parameters = parseParameters(parameterNodeList, parameterNames);

        parsedAlert.setEventId(parameters.get(EVENT_ID));
        parsedAlert.setEventType(parameters.get(EVENT_TYPE));
        parsedAlert.setCurrentEpisodeId(parameters.get(CURRENT_EPISODE_ID));
        parsedAlert.setFromDate(parseDateTimeFromString(parameters.get(FROM_DATE)));
        parsedAlert.setToDate(parseDateTimeFromString(parameters.get(TO_DATE)));
        parsedAlert.setLink(parameters.get(LINK));

        return parsedAlert;
    }

    private Document getXmlDocument(String xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
        DocumentBuilder builder = builderFactory.newDocumentBuilder();
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        return builder.parse(inputStream);
    }

    private Map<String, String> parseParameters(NodeList parameterNodes, Set<String> parameterNames) {
        Map<String, String> parameters = parameterNames.stream().map(name -> Map.entry(name, ""))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        for (int i = 0; i < parameterNodes.getLength(); i++) {
            NodeList parameterChildNodes = parameterNodes.item(i).getChildNodes();
            String parameterName = getNodeValueByName(parameterChildNodes, VALUE_NAME);
            if (parameters.containsKey(parameterName)) {
                parameters.replace(parameterName, getNodeValueByName(parameterChildNodes, VALUE));
            }
        }
        return parameters;
    }

    private String getNodeValueByName(NodeList nodes, String childNodeName) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (StringUtils.equals(node.getLocalName(), childNodeName)) {
                return node.getTextContent();
            }
        }
        return "";
    }

    private String getValueByTagName(String xml, Document xmlDocument, String tagName) {
        NodeList nodeList = xmlDocument.getElementsByTagNameNS(NS, tagName);
        if (nodeList.getLength() == 0) {
            LOG.warn("Alert does not contain tag '{}': \n" + xml, tagName);
            return "";
        }
        if (nodeList.getLength() > 1) {
            LOG.warn("Alert contains more than one tag '{}': \n" + xml, tagName);
        }
        return nodeList.item(0).getTextContent();
    }
}
