package io.kontur.eventapi.calfire.episodecomposition;

import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.WildfireEpisodeCombinator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static io.kontur.eventapi.calfire.converter.CalFireDataLakeConverter.CALFIRE_PROVIDER;
import static io.kontur.eventapi.util.GeometryUtil.isEqualGeometries;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

@Component
public class CalFireEpisodeCombinator extends WildfireEpisodeCombinator {

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return observation.getProvider().equals(CALFIRE_PROVIDER);
    }

    @Override
    public List<FeedEpisode> processObservation(NormalizedObservation observation, FeedData feedData, Set<NormalizedObservation> eventObservations) {
        FeedEpisode episode = createDefaultEpisode(observation);
        List<FeedEpisode> existing = feedData.getEpisodes();
        if (!existing.isEmpty() && sameEpisode(existing.get(existing.size() - 1), episode)) {
            return emptyList();
        }
        return List.of(episode);
    }

    private boolean sameEpisode(FeedEpisode ep1, FeedEpisode ep2) {
        return equalsIgnoreCase(ep1.getName(), ep2.getName())
                && ep1.getSeverity() == ep2.getSeverity()
                && equalsIgnoreCase(ep1.getLocation(), ep2.getLocation())
                && isEqualGeometries(ep1.getGeometries(), ep2.getGeometries());
    }
}
