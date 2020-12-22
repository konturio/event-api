package io.kontur.eventapi.gdacs.normalization;

import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.normalization.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_GEOMETRY_PROVIDER;
import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_PROVIDER;

@Component
public class GdacsGeometryNormalizer extends Normalizer {

    private final static Logger LOG = LoggerFactory.getLogger(GdacsGeometryNormalizer.class);

    private final NormalizedObservationsDao normalizedObservationsDao;

    @Autowired
    public GdacsGeometryNormalizer(NormalizedObservationsDao normalizedObservationsDao) {
        this.normalizedObservationsDao = normalizedObservationsDao;
    }

    @Override
    public boolean isApplicable(DataLake dataLakeDto) {
        return GDACS_ALERT_GEOMETRY_PROVIDER.equals(dataLakeDto.getProvider());
    }

    @Override
    public NormalizedObservation normalize(DataLake dataLakeDto) {
        var normalizedObservation = getGdacsAlertNormalizedObservation(dataLakeDto.getExternalId());
        if (normalizedObservation.isPresent()) {
            var normalizedObservationWithGeometry = new NormalizedObservation();
            setDataFromNormalizedObservation(normalizedObservationWithGeometry, normalizedObservation.get());
            setDataFromDataLake(normalizedObservationWithGeometry, dataLakeDto);
            return normalizedObservationWithGeometry;
        }
        throw new RuntimeException(String.format("Observation with provider = %s and externalId = %s has not normalized", GDACS_ALERT_PROVIDER, dataLakeDto.getExternalId()));
    }

    private void setDataFromNormalizedObservation(NormalizedObservation normalizedObservationWithGeometry, NormalizedObservation observation) {
        normalizedObservationWithGeometry.setSourceUpdatedAt(observation.getSourceUpdatedAt());
        normalizedObservationWithGeometry.setName(observation.getName());
        normalizedObservationWithGeometry.setDescription(observation.getDescription());
        normalizedObservationWithGeometry.setEpisodeDescription(observation.getEpisodeDescription());
        normalizedObservationWithGeometry.setType(observation.getType());
        normalizedObservationWithGeometry.setEventSeverity(observation.getEventSeverity());
        normalizedObservationWithGeometry.setExternalEventId(observation.getExternalEventId());
        normalizedObservationWithGeometry.setExternalEpisodeId(observation.getExternalEpisodeId());
        normalizedObservationWithGeometry.setStartedAt(observation.getStartedAt());
        normalizedObservationWithGeometry.setEndedAt(observation.getEndedAt());
        normalizedObservationWithGeometry.setActive(observation.getActive());
    }

    private void setDataFromDataLake(NormalizedObservation normalizedObservationWithGeometry, DataLake dataLake) {
        normalizedObservationWithGeometry.setObservationId(dataLake.getObservationId());
        normalizedObservationWithGeometry.setProvider(dataLake.getProvider());
        normalizedObservationWithGeometry.setLoadedAt(dataLake.getLoadedAt());
        normalizedObservationWithGeometry.setGeometries(dataLake.getData());
    }

    private Optional<NormalizedObservation> getGdacsAlertNormalizedObservation(String externalEventId) {
        return normalizedObservationsDao.getNormalizedObservationByExternalEpisodeIdAndProvider(externalEventId, GDACS_ALERT_PROVIDER);
    }
}
