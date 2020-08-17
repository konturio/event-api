package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.dto.FeedDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface FeedMapper {

    List<FeedDto> getFeeds();

    int insertFeedData(
            UUID eventId, UUID feedId, Long version, String name, String description,
            @Param("observations") String observations, @Param("episodes") String episodes);

}
