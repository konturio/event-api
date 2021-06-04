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


/**
 * GdacsAlert and GdacsAlertGeometry observations are combined into one episode
 * but FeedComposition job processes each observation separately.
 * So Geometry observation is skipped during FeedComposition job and full episode
 * is constructed while FeedComposition is running for Alert observation.
 *
 * GdacsGeometryEpisodeCombinator is used to skip adding new episode for Geometry
 * observation.
 */
@Component
public class GdacsGeometryEpisodeCombinator extends EpisodeCombinator {

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return observation.getProvider().equals(GDACS_ALERT_GEOMETRY_PROVIDER);
    }

    @Override
    public Optional<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations) {
        if (isAlertObservationPresentForGeometry(observation, eventObservations)) {
            return Optional.empty();
        }
        throw new RuntimeException("Alert observation not found for geometry: " + observation.getObservationId());
    }

    private boolean isAlertObservationPresentForGeometry(NormalizedObservation geometryObservation, Set<NormalizedObservation> eventObservations) {
        return eventObservations.stream()
                .anyMatch(obs -> obs.getProvider().equals(GDACS_ALERT_PROVIDER)
                        && obs.getExternalEpisodeId().equals(geometryObservation.getExternalEpisodeId())
                        && obs.getSourceUpdatedAt().equals(geometryObservation.getSourceUpdatedAt()));
    }
}
