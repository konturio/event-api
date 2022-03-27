package io.kontur.eventapi.gdacs.normalization;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.gdacs.converter.GdacsAlertXmlParser;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.List;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_PROVIDER;
import static java.lang.String.format;


@Component
public class GdacsAlertNormalizer extends GdacsNormalizer {

    private final GdacsAlertXmlParser parser;

    @Autowired
    public GdacsAlertNormalizer(GdacsAlertXmlParser parser) {
        this.parser = parser;
    }

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return GDACS_ALERT_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        var normalizedObservation = new NormalizedObservation();
        try {
            ParsedAlert parsedAlert = parser.getParsedAlertToNormalization(dataLakeDto.getData());
            normalizedObservation.setActive(true);
            setDataFromDataLakeDto(normalizedObservation, dataLakeDto);
            setDataFromParsedAlert(normalizedObservation, parsedAlert);
            return normalizedObservation;
        } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException e) {
            throw new RuntimeException(format("Alert can not be parsed %s", dataLakeDto.getObservationId()));
        }
    }

    private void setDataFromDataLakeDto(NormalizedObservation normalizedObservation, DataLake dataLakeDto) {
        normalizedObservation.setProvider(dataLakeDto.getProvider());
        normalizedObservation.setObservationId(dataLakeDto.getObservationId());
        normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
        normalizedObservation.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());
        normalizedObservation.setExternalEpisodeId(dataLakeDto.getExternalId());
    }

    private void setDataFromParsedAlert(NormalizedObservation normalizedObservation, ParsedAlert parsedAlert) {

        normalizedObservation.setName(parsedAlert.getHeadLine());
        normalizedObservation.setProperName(parsedAlert.getEventName().trim());
        normalizedObservation.setDescription(parsedAlert.getDescription());
        normalizedObservation.setEpisodeDescription(parsedAlert.getDescription());
        normalizedObservation.setType(defineType(parsedAlert.getEvent()));
        normalizedObservation.setEventSeverity(defineSeverity(parsedAlert.getSeverity()));
        normalizedObservation.setExternalEventId(composeExternalEventId(parsedAlert.getEventType(), parsedAlert.getEventId()));
        normalizedObservation.setStartedAt(parsedAlert.getFromDate());
        normalizedObservation.setEndedAt(parsedAlert.getToDate());
        if (StringUtils.isNotBlank(parsedAlert.getLink())) {
            normalizedObservation.setSourceUri(List.of(parsedAlert.getLink()));
        }
        normalizedObservation.setRegion(parsedAlert.getCountry());
    }
}
