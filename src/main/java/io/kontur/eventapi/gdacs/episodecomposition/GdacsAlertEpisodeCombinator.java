package io.kontur.eventapi.gdacs.episodecomposition;

import io.kontur.eventapi.dao.NormalizedObservationsDao;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_GEOMETRY_PROVIDER;
import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_PROVIDER;

@Component
public class GdacsAlertEpisodeCombinator extends EpisodeCombinator {
    private final NormalizedObservationsDao observationsDao;

    public GdacsAlertEpisodeCombinator(NormalizedObservationsDao observationsDao) {
        this.observationsDao = observationsDao;
    }

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return observation.getProvider().equals(GDACS_ALERT_GEOMETRY_PROVIDER);
    }

    @Override
    public Optional<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData) {
        Optional<FeedEpisode> episodeOpt = createDefaultEpisode(observation);
        episodeOpt.ifPresent(episode -> addObservationIdIntoEpisode(episode, observation));
        return episodeOpt;
    }

    private void addObservationIdIntoEpisode(FeedEpisode feedEpisode, NormalizedObservation observation) {
        var gdacsAlertObservation = observationsDao.getNormalizedObservationByExternalEpisodeIdAndProvider(
                observation.getExternalEpisodeId(), GDACS_ALERT_PROVIDER);
        gdacsAlertObservation.ifPresent(
                normalizedObservation -> feedEpisode.addObservation(normalizedObservation.getObservationId())
        );
    }
}
