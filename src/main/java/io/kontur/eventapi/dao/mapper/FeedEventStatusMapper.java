package io.kontur.eventapi.dao.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

@Mapper
public interface FeedEventStatusMapper {

    void markAsActual(@Param("feedId") UUID feedId,
                      @Param("eventId") UUID eventId,
                      @Param("actual") boolean actual);

    void markAsNonActual(@Param("provider") String provider,
                         @Param("eventId") UUID eventId);

    void markAsNonActualExcludeFeed(@Param("provider") String provider,
                                    @Param("eventId") UUID eventId,
                                    @Param("excludeAlias") String excludeAlias);
}
