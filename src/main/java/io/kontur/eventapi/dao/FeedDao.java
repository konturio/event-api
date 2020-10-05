package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.FeedMapper;
import io.kontur.eventapi.entity.FeedData;
import io.kontur.eventapi.entity.Feed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

import static io.kontur.eventapi.util.JsonUtil.writeJson;

@Component
public class FeedDao {

    private final FeedMapper mapper;

    @Autowired
    public FeedDao(FeedMapper mapper) {
        this.mapper = mapper;
    }

    public List<Feed> getFeeds() {
        return mapper.getFeeds();
    }

    public void insertFeedData(FeedData feedData) {
        String episodesJson = writeJson(feedData.getEpisodes());
        mapper.insertFeedData(feedData.getEventId(), feedData.getFeedId(), feedData.getVersion(),
                feedData.getName(), feedData.getDescription(),
                feedData.getStartedAt(), feedData.getEndedAt(), feedData.getUpdatedAt(),
                feedData.getObservations(), episodesJson);
    }

    public List<FeedData> searchForEvents(String feedAlias, OffsetDateTime after, int offset,
                                          int limit) {
        return mapper.searchForEvents(feedAlias, after, offset, limit);
    }

}
