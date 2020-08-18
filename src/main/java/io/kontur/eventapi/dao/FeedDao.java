package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.FeedMapper;
import io.kontur.eventapi.dto.FeedDataDto;
import io.kontur.eventapi.dto.FeedDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static io.kontur.eventapi.util.JsonUtil.writeJson;

@Component
public class FeedDao {

    private final FeedMapper mapper;

    @Autowired
    public FeedDao(FeedMapper mapper) {
        this.mapper = mapper;
    }

    public List<FeedDto> getFeeds() {
        return mapper.getFeeds();
    }

    public void insertFeedData(FeedDataDto feedData) {
        String episodesJson = writeJson(feedData.getEpisodes());
        mapper.insertFeedData(feedData.getEventId(), feedData.getFeedId(), feedData.getVersion(),
                feedData.getName(), feedData.getDescription(),
                feedData.getStartedAt(), feedData.getEndedAt(), feedData.getEndedAt(),
                null, episodesJson);
    }

    public List<FeedDataDto> searchForEvents(OffsetDateTime after, int offset, int limit) {
        return mapper.searchForEvents(
                UUID.fromString("10f240fa-6116-441b-bb68-6649162ca506"), //TODO define UUID from user roles
                after, offset, limit);
    }

}
