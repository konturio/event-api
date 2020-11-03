package io.kontur.eventapi.gdacs.normalization;

import feign.FeignException;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.gdacs.client.GdacsClient;
import io.kontur.eventapi.normalization.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_PROVIDER;
import static io.kontur.eventapi.util.DateTimeUtil.parseDateTimeFromString;

@Component
public class GdacsNormalizer extends Normalizer {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsNormalizer.class);

    private final GdacsClient gdacsClient;

    @Autowired
    public GdacsNormalizer(GdacsClient gdacsClient) {
        this.gdacsClient = gdacsClient;
    }

    private static final Map<String, EventType> typeMap = Map.of(
            "Drought", EventType.DROUGHT,
            "Earthquake", EventType.EARTHQUAKE,
            "Flood", EventType.FLOOD,
            "Tropical Cyclone", EventType.CYCLONE,
            "Volcano Eruption", EventType.VOLCANO
    );

    private static final Map<String, Severity> severityMap = Map.of(
            "Minor", Severity.MINOR,
            "Moderate", Severity.MODERATE,
            "Severe", Severity.SEVERE,
            "Extreme", Severity.EXTREME
    );

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return GDACS_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        var normalizedObservation = new NormalizedObservation();

        normalizedObservation.setProvider(dataLakeDto.getProvider());
        normalizedObservation.setExternalEventId(dataLakeDto.getExternalId());
        normalizedObservation.setObservationId(dataLakeDto.getObservationId());
        normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
        normalizedObservation.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());

        normalizedObservation.setActive(true);
        try {
            parseData(normalizedObservation, dataLakeDto);
        } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException e) {
            LOG.warn("Alert can not be parsed {}", dataLakeDto.getObservationId());
            throw new RuntimeException(e);
        } catch (FeignException e) {
            LOG.warn("Did not found geometry for alert {}", dataLakeDto.getObservationId());
            throw new IllegalArgumentException(e);
        }

        return normalizedObservation;
    }

    private void parseData(NormalizedObservation normalizedObservation,
                           DataLake dataLakeDto) throws ParserConfigurationException,
            IOException, SAXException, XPathExpressionException, FeignException {

        var builderFactory = DocumentBuilderFactory.newInstance();
        var builder = builderFactory.newDocumentBuilder();
        var inputStream = new ByteArrayInputStream(dataLakeDto.getData().getBytes());
        var xmlDocument = builder.parse(inputStream);
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

        normalizedObservation.setName(headline);
        normalizedObservation.setDescription(description);
        normalizedObservation.setEpisodeDescription(description);

        normalizedObservation.setType(typeMap.getOrDefault(event, EventType.OTHER));
        normalizedObservation.setEventSeverity(severityMap.getOrDefault(severity, Severity.UNKNOWN));

        var parameterNodeList = (NodeList) xPath.compile(pathToParameters)
                .evaluate(xmlDocument, XPathConstants.NODESET);
        setDataFromParameters(normalizedObservation, parameterNodeList, xmlDocument, xPath);
    }

    private void setDataFromParameters(NormalizedObservation normalizedObservation, NodeList parameterNodeList,
                                       Document xmlDocument,
                                       XPath xPath) throws XPathExpressionException, FeignException {
        String eventid = "";
        String currentepisodeid = "";
        String eventtype = "";
        for (int i = 0; i < parameterNodeList.getLength(); i++) {
            int indexOfParameters = i + 1;
            String pathToValueName = "/alert/info/parameter[" + indexOfParameters + "]/valueName/text()";
            var valueName = (String) xPath.compile(pathToValueName).evaluate(xmlDocument, XPathConstants.STRING);

            switch (valueName) {
                case "fromdate":
                    String fromDate = getValue(indexOfParameters, xmlDocument, xPath);
                    normalizedObservation.setStartedAt(parseDateTimeFromString(fromDate));
                    break;
                case "todate":
                    String toDate = getValue(indexOfParameters, xmlDocument, xPath);
                    normalizedObservation.setEndedAt(parseDateTimeFromString(toDate));
                    break;
                case "eventid":
                    eventid = getValue(indexOfParameters, xmlDocument, xPath);
                    break;
                case "currentepisodeid":
                    currentepisodeid = getValue(indexOfParameters, xmlDocument, xPath);
                    break;
                case "eventtype":
                    eventtype = getValue(indexOfParameters, xmlDocument, xPath);
                    break;
            }
        }

        String geometry = gdacsClient.getGeometryByLink(eventtype, eventid, currentepisodeid);
        normalizedObservation.setGeometries(geometry);
    }

    private String getValue(int indexOfParameters, Document xmlDocument, XPath xPath) throws XPathExpressionException {
        String pathToUpdateDate = "/alert/info/parameter[" + indexOfParameters + "]/value/text()";
        return (String) xPath.compile(pathToUpdateDate).evaluate(xmlDocument, XPathConstants.STRING);
    }
}
