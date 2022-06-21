package io.kontur.eventapi.gdacs.converter;

import feign.FeignException;
import io.kontur.eventapi.cap.converter.CapBaseXmlParser;
import io.kontur.eventapi.cap.dto.CapParsedEvent;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.*;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static io.kontur.eventapi.util.DateTimeUtil.parseDateTimeFromString;

@Component
public class GdacsAlertXmlParser extends CapBaseXmlParser {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsAlertXmlParser.class);

    public static final String IDENTIFIER = "identifier";

    private static final String DATE_MODIFIED = "datemodified";
    private static final String EVENT_ID = "eventid";
    private static final String EVENT_TYPE = "eventtype";
    private static final String CURRENT_EPISODE_ID = "currentepisodeid";
    private static final String FROM_DATE = "fromdate";
    private static final String TO_DATE = "todate";
    private static final String LINK = "link";
    private static final String EVENT_NAME = "eventname";
    private static final String COUNTRY = "country";
    private static final String EVENT = "event";
    private static final String HEADLINE = "headline";
    private static final String SEVERITY = "severity";
    private static final String DESCRIPTION = "description";
    private static final String PARAMETER = "parameter";
    private static final String ALERT = "alert";
    private static final String VALUE = "value";
    private static final String VALUE_NAME = "valueName";


    @Override
    public Optional<CapParsedEvent> getParsedItemForDataLake(String xml, String provider) {
        String externalId = "";
        try {
            Document xmlDocument = getXmlDocument(xml);

            externalId = getValueByTagName(xmlDocument, IDENTIFIER);
            if (StringUtils.isEmpty(externalId)) {
                LOG.warn("Alert does not have identifier: \n" +  xml);
                return Optional.empty();
            }

            NodeList parameterNodeList = xmlDocument.getElementsByTagNameNS(getNamespace(), PARAMETER);
            Set<String> parameterNames = Set.of(DATE_MODIFIED, EVENT_ID, EVENT_TYPE, CURRENT_EPISODE_ID);
            Map<String, String> parameters = parseParameters(parameterNodeList, parameterNames);

            if (parameters.values().stream().anyMatch(StringUtils::isEmpty)) {
                LOG.warn("Alert with id {} does not have parameters", externalId);
                return Optional.empty();
            }

            return Optional.of(new ParsedAlert(
                    OffsetDateTime.parse(parameters.get(DATE_MODIFIED), DateTimeFormatter.RFC_1123_DATE_TIME),
                    externalId,
                    parameters.get(EVENT_ID),
                    parameters.get(EVENT_TYPE),
                    parameters.get(CURRENT_EPISODE_ID),
                    xml));

        } catch (IOException | SAXException e) {
            LOG.warn("Alert is not valid and can not be parsed: id {}", externalId, e);
        } catch (DateTimeParseException e) {
            LOG.warn("Alert value of parameter 'datemodified' can not be parsed: id {}", externalId, e);
        } catch (ParserConfigurationException e) {
            LOG.warn("Parser configuration error while parse gdacs alert: id {}", externalId, e);
        }
        return Optional.empty();
    }

    public ParsedAlert getParsedAlertToNormalization(String xml) throws ParserConfigurationException,
            IOException, SAXException, XPathExpressionException, FeignException {

        Document xmlDocument = getXmlDocument(xml);
        ParsedAlert parsedAlert = new ParsedAlert();

        parsedAlert.setHeadLine(getValueByTagName(xmlDocument, HEADLINE));
        parsedAlert.setDescription(getValueByTagName(xmlDocument, DESCRIPTION));
        parsedAlert.setEvent(getValueByTagName(xmlDocument, EVENT));
        parsedAlert.setSeverity(getValueByTagName(xmlDocument, SEVERITY));

        NodeList parameterNodeList = xmlDocument.getElementsByTagNameNS(getNamespace(), PARAMETER);
        Set<String> parameterNames = Set.of(EVENT_ID, EVENT_TYPE, CURRENT_EPISODE_ID, FROM_DATE, TO_DATE, LINK,
                EVENT_NAME, COUNTRY);
        Map<String, String> parameters = parseParameters(parameterNodeList, parameterNames);

        parsedAlert.setEventId(parameters.get(EVENT_ID));
        parsedAlert.setEventType(parameters.get(EVENT_TYPE));
        parsedAlert.setCurrentEpisodeId(parameters.get(CURRENT_EPISODE_ID));
        parsedAlert.setFromDate(parseDateTimeFromString(parameters.get(FROM_DATE)));
        parsedAlert.setToDate(parseDateTimeFromString(parameters.get(TO_DATE)));
        parsedAlert.setLink(parameters.get(LINK));
        parsedAlert.setEventName(parameters.get(EVENT_NAME));
        parsedAlert.setCountry(parameters.get(COUNTRY));

        return parsedAlert;
    }

    protected Map<String, String> parseParameters(NodeList parameterNodes, Set<String> parameterNames) {
        return parseParameters(parameterNodes, parameterNames, VALUE_NAME, VALUE);
    }

    protected String getItemName() {
        return ALERT;
    }

    protected String getIdTagName() {
        return IDENTIFIER;
    }

}
