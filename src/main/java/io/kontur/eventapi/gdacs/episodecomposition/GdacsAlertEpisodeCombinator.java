package io.kontur.eventapi.gdacs.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.EpisodeCombinator;
import io.kontur.eventapi.job.exception.FeedCompositionSkipException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_GEOMETRY_PROVIDER;
import static io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter.GDACS_ALERT_PROVIDER;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toSet;
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
    public List<FeedEpisode> processObservation(NormalizedObservation observation, FeedData event, Set<NormalizedObservation> eventObservations) {
        if (event.getEpisodes().size() > 0) return emptyList();

        Map.Entry<NormalizedObservation, NormalizedObservation> observations = checkObservationsAndGetLatest(eventObservations, event);

        NormalizedObservation alertObservation = observations.getKey();
        NormalizedObservation geometryObservation = observations.getValue();

        FeedEpisode episode = createDefaultEpisode(alertObservation);
        episode.setObservations(findObservationsForEpisode(eventObservations));
        episode.setGeometries(geometryObservation.getGeometries());
        if (isBlank(episode.getProperName())) {
            episode.setProperName(geometryObservation.getProperName());
        }
        if (isBlank(episode.getLocation())) {
            episode.setLocation(geometryObservation.getRegion());
        }
        episode.setSeverityData(geometryObservation.getSeverityData());
        return List.of(episode);
    }

    private Map.Entry<NormalizedObservation, NormalizedObservation> checkObservationsAndGetLatest(Set<NormalizedObservation> eventObservations, FeedData event) {
        return eventObservations
                .stream()
                .filter(obs -> obs.getProvider().equals(GDACS_ALERT_PROVIDER))
                .map(obs -> Map.entry(obs, getGeometryObservationForAlert(obs, eventObservations)))
                .max(comparing(obs -> obs.getKey().getLoadedAt()))
                .orElseThrow(() -> new FeedCompositionSkipException(format(
                        "No alert observation present for event: event_id = '%s', feed_id = '%s', version = %d",
                        event.getFeedId(), event.getEventId(), event.getVersion())));
    }

    private NormalizedObservation getGeometryObservationForAlert(NormalizedObservation alertObservation, Set<NormalizedObservation> eventObservations) {
        return eventObservations.stream()
                .filter(obs -> obs.getProvider().equals(GDACS_ALERT_GEOMETRY_PROVIDER)
                        && obs.getExternalEpisodeId().equals(alertObservation.getExternalEpisodeId())
                        && obs.getSourceUpdatedAt().equals(alertObservation.getSourceUpdatedAt()))
                .findFirst()
                .orElseThrow(() -> new FeedCompositionSkipException("Geometry observation not found for alert: " + alertObservation.getObservationId()));
    }

    private Set<UUID> findObservationsForEpisode(Set<NormalizedObservation> eventObservations) {
        return eventObservations
                .stream()
                .map(NormalizedObservation::getObservationId)
                .collect(toSet());
    }
}
