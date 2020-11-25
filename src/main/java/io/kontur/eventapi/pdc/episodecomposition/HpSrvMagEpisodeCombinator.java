package io.kontur.eventapi.pdc.episodecomposition;

import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import org.springframework.stereotype.Component;
import org.wololo.geojson.FeatureCollection;

import java.util.Optional;
import java.util.UUID;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_GEOMETRY_PROVIDER;
import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_PROVIDER;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.HP_SRV_MAG_PROVIDER;
import static io.kontur.eventapi.pdc.converter.PdcDataLakeConverter.PDC_SQS_PROVIDER;
import static io.kontur.eventapi.util.JsonUtil.readJson;

@Component
public class HpSrvMagEpisodeCombinator extends EpisodeCombinator {
    private final NormalizedObservationsDao observationsDao;

    public HpSrvMagEpisodeCombinator(NormalizedObservationsDao observationsDao) {
        this.observationsDao = observationsDao;
    }

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return observation.getProvider().equals(HP_SRV_MAG_PROVIDER);
    }

    @Override
    public Optional<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData) {
        var savedDuplicateObservationId = getSavedDuplicateSqsObservationId(observation);
        if (savedDuplicateObservationId.isPresent()) {
            addObservationIdIfDuplicate(observation, feedData, savedDuplicateObservationId.get());
            return Optional.empty();
        }
        return createDefaultEpisode(observation);
    }

    private Optional<UUID> getSavedDuplicateSqsObservationId(NormalizedObservation observation) {
        var duplicateSQSMagObservationOpt = observationsDao.getDuplicateObservation(
                observation.getLoadedAt(),
                observation.getExternalEpisodeId(),
                observation.getObservationId(),
                PDC_SQS_PROVIDER);

        return duplicateSQSMagObservationOpt.map(NormalizedObservation::getObservationId);
    }

    private void addObservationIdIfDuplicate(NormalizedObservation observation, FeedData feedDto, UUID savedDuplicateObservationId) {
        for (FeedEpisode episode : feedDto.getEpisodes()) {
            boolean hasDuplicateObservation = episode.getObservations().stream()
                    .anyMatch(episodeObs -> episodeObs.equals(savedDuplicateObservationId));
            if (hasDuplicateObservation) {
                episode.addObservation(observation.getObservationId());
                return;
            }
        }
    }
}
