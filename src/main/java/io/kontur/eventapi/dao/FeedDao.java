package io.kontur.eventapi.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.kontur.eventapi.dao.mapper.FeedMapper;
import io.kontur.eventapi.dto.FeedDataDto;
import io.kontur.eventapi.dto.FeedDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.kontur.eventapi.util.JsonUtil.writeJson;

@Component
public class FeedDao {

    private final ObjectMapper objectMapper = new ObjectMapper();
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

}
