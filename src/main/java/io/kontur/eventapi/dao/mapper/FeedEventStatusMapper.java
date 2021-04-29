package io.kontur.eventapi.dao.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.UUID;

@Mapper
public interface FeedEventStatusMapper {

    void markAsActual(UUID feedId, UUID eventId, boolean actual);

    void markAsNonActual(String provider, UUID eventId);
}
