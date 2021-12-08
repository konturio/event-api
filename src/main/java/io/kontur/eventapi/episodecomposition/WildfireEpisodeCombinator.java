package io.kontur.eventapi.episodecomposition;

import static java.util.Comparator.comparing;

import java.time.OffsetDateTime;
import java.util.List;

import io.kontur.eventapi.entity.FeedEpisode;

public abstract class WildfireEpisodeCombinator extends EpisodeCombinator {

    @Override
    public List<FeedEpisode> postProcessEpisodes(List<FeedEpisode> episodes) {
        if (episodes.size() < 2) return episodes;

        episodes.sort(comparing(FeedEpisode::getStartedAt).thenComparing(FeedEpisode::getEndedAt));
        OffsetDateTime lastEndedAt = null;
        for (FeedEpisode episode : episodes) {
            if (lastEndedAt != null
                    && lastEndedAt.isAfter(episode.getStartedAt())
                    && lastEndedAt.isBefore(episode.getEndedAt())) {
                episode.setStartedAt(lastEndedAt);
            }
            lastEndedAt = episode.getEndedAt();
        }
        return episodes;
    }
}
