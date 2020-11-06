package io.kontur.eventapi.gdacs.normalization;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.EventType;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.entity.Severity;
import io.kontur.eventapi.gdacs.converter.GdacsAlertXmlParser;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import io.kontur.eventapi.normalization.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static io.kontur.eventapi.gdacs.job.GdacsSearchJob.GDACS_PROVIDER;

@Component
public class GdacsNormalizer extends Normalizer {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsNormalizer.class);

    private final GdacsAlertXmlParser parser;
    private final DataLakeDao dataLakeDao;

    @Autowired
    public GdacsNormalizer(GdacsAlertXmlParser parser, DataLakeDao dataLakeDao) {
        this.parser = parser;
        this.dataLakeDao = dataLakeDao;
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
        try {
            var geometry = getGeometryFromDataLake(dataLakeDto.getExternalId());
            if (geometry.isPresent()) {
                var parsedAlert = parser.getParsedAlertToNormalization(dataLakeDto.getData());
                normalizedObservation.setActive(true);
                setDataFromDataLakeDto(normalizedObservation, dataLakeDto);
                normalizedObservation.setGeometries(geometry.get());
                getDataFromParsedAlert(normalizedObservation, parsedAlert);
                return normalizedObservation;
            }
            LOG.warn("Gdacs alert geometry has not found in data_lake, observationId = {}", dataLakeDto.getObservationId());
            return null;
        } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException e) {
            LOG.warn("Alert can not be parsed {}", dataLakeDto.getObservationId());
            throw new RuntimeException(e);
        }
    }

    private void setDataFromDataLakeDto(NormalizedObservation normalizedObservation, DataLake dataLakeDto) {
        normalizedObservation.setProvider(dataLakeDto.getProvider());
        normalizedObservation.setObservationId(dataLakeDto.getObservationId());
        normalizedObservation.setLoadedAt(dataLakeDto.getLoadedAt());
        normalizedObservation.setSourceUpdatedAt(dataLakeDto.getUpdatedAt());
    }

    private Optional<String> getGeometryFromDataLake(String externalId) {
        var dataLake = dataLakeDao.getDataLakeWithGeometryForGdacs(externalId);
        return dataLake.map(DataLake::getData);
    }

    private void getDataFromParsedAlert(NormalizedObservation normalizedObservation, ParsedAlert parsedAlert) {

        normalizedObservation.setName(parsedAlert.getHeadLine());
        normalizedObservation.setDescription(parsedAlert.getDescription());
        normalizedObservation.setEpisodeDescription(parsedAlert.getDescription());
        normalizedObservation.setType(typeMap.getOrDefault(parsedAlert.getEvent(), EventType.OTHER));
        normalizedObservation.setEventSeverity(severityMap.getOrDefault(parsedAlert.getSeverity(), Severity.UNKNOWN));

        normalizedObservation.setExternalEventId(parsedAlert.getEventType() + "_" + parsedAlert.getEventId());

        normalizedObservation.setStartedAt(parsedAlert.getFromDate());
        normalizedObservation.setEndedAt(parsedAlert.getToDate());

    }
}
