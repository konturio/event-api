package io.kontur.eventapi.gdacs.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_GEOMETRY_PROVIDER;
import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_PROVIDER;
import static org.apache.commons.lang3.StringUtils.isBlank;


/**
 * GdacsAlertEpisodeCombinator combines GdacsAlert and GdacsAlertGeometry
 * into one episode.
 */
@Component
public class GdacsAlertEpisodeCombinator extends EpisodeCombinator {

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return observation.getProvider().equals(GDACS_ALERT_PROVIDER);
    }

    @Override
    public Optional<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations) {
        Optional<NormalizedObservation> geometryObservation = getGeometryObservationForAlert(observation, eventObservations);
        if (geometryObservation.isPresent()) {
            Optional<FeedEpisode> feedEpisode = createDefaultEpisode(observation);
            feedEpisode.ifPresent(episode -> {
                episode.setGeometries(geometryObservation.get().getGeometries());
                episode.addObservation(geometryObservation.get().getObservationId());
                episode.addUrlIfNotExists(geometryObservation.get().getSourceUri());
                if (isBlank(episode.getProperName())) {
                    episode.setProperName(geometryObservation.get().getProperName());
                }
            });
            return feedEpisode;
        }
        throw new RuntimeException("Geometry observation not found for alert: " + observation.getObservationId());
    }

    private Optional<NormalizedObservation> getGeometryObservationForAlert(NormalizedObservation alertObservation, Set<NormalizedObservation> eventObservations) {
        return eventObservations.stream()
                .filter(obs -> obs.getProvider().equals(GDACS_ALERT_GEOMETRY_PROVIDER)
                        && obs.getExternalEpisodeId().equals(alertObservation.getExternalEpisodeId())
                        && obs.getSourceUpdatedAt().equals(alertObservation.getSourceUpdatedAt()))
                .findFirst();
    }
}
