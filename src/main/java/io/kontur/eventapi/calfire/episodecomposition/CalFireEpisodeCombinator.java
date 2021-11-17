package io.kontur.eventapi.calfire.episodecomposition;

import io.kontur.eventapi.entity.FeedEpisode;
import io.kontur.eventapi.entity.NormalizedObservation;
import io.kontur.eventapi.episodecomposition.DefaultEpisodeCombinator;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;

import static io.kontur.eventapi.calfire.converter.CalFireDataLakeConverter.CALFIRE_PROVIDER;

@Component
public class CalFireEpisodeCombinator extends DefaultEpisodeCombinator {

    public CalFireEpisodeCombinator() {
        super();
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public boolean isApplicable(NormalizedObservation observation) {
        return observation.getProvider().equals(CALFIRE_PROVIDER);
    }

    @Override
    public List<FeedEpisode> postProcessEpisodes(List<FeedEpisode> episodes) {
        if (episodes.size() > 1) {
            final OffsetDateTime[] startedAt = {null};
            episodes.forEach(obs -> {
                if (startedAt[0] != null
                        && startedAt[0].isAfter(obs.getStartedAt())
                        && startedAt[0].isBefore(obs.getEndedAt())) {
                    obs.setStartedAt(startedAt[0]);
                }
                startedAt[0] = obs.getEndedAt();
            });
        }
        return episodes;
    }

}
