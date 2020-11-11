package io.kontur.eventapi.gdacs.normalization;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.gdacs.converter.GdacsAlertXmlParser;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.Optional;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_GEOMETRY_PROVIDER;
import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_PROVIDER;


@Component
public class GdacsAlertNormalizer extends GdacsNormalizer {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsAlertNormalizer.class);

    private final GdacsAlertXmlParser parser;
    private final DataLakeDao dataLakeDao;

    @Autowired
    public GdacsAlertNormalizer(GdacsAlertXmlParser parser, DataLakeDao dataLakeDao) {
        this.parser = parser;
        this.dataLakeDao = dataLakeDao;
    }

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return GDACS_ALERT_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        var normalizedObservation = new NormalizedObservation();
        try {
            var geometry = checkObservationWithGeometry(dataLakeDto.getExternalId());
            if (geometry.isPresent()) {
                var parsedAlert = parser.getParsedAlertToNormalization(dataLakeDto.getData());
                normalizedObservation.setActive(true);
                setDataFromDataLakeDto(normalizedObservation, dataLakeDto);
                setDataFromParsedAlert(normalizedObservation, parsedAlert);
                return normalizedObservation;
            }
            LOG.warn("Gdacs alert geometry has not found in data_lake, observationId = {}", dataLakeDto.getObservationId());
            return null;
        } catch (ParserConfigurationException | IOException | SAXException | XPathExpressionException e) {
            LOG.warn("Alert can not be parsed {}", dataLakeDto.getObservationId());
            throw new RuntimeException(e);
        }
    }

    private Optional<String> checkObservationWithGeometry(String externalId) {
        var dataLake = dataLakeDao.getDataLakeByExternalIdAndProvider(externalId, GDACS_ALERT_GEOMETRY_PROVIDER);
        return dataLake.map(DataLake::getData);
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
        normalizedObservation.setDescription(parsedAlert.getDescription());
        normalizedObservation.setEpisodeDescription(parsedAlert.getDescription());
        normalizedObservation.setType(defineType(parsedAlert.getEvent()));
        normalizedObservation.setEventSeverity(defineSeverity(parsedAlert.getSeverity()));

        normalizedObservation.setExternalEventId(parsedAlert.getEventType() + "_" + parsedAlert.getEventId());

        normalizedObservation.setStartedAt(parsedAlert.getFromDate());
        normalizedObservation.setEndedAt(parsedAlert.getToDate());

    }
}
