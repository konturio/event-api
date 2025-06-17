package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.UserFeedSettings;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;
import java.util.List;

@Mapper
public interface UserFeedSettingsMapper {

    Optional<UserFeedSettings> getUserFeedSettings(@Param("userName") String userName);

    void upsertUserFeedSettings(@Param("userName") String userName,
                                @Param("feeds") List<String> feeds,
                                @Param("defaultFeed") String defaultFeed);

    void updateDefaultFeed(@Param("userName") String userName,
                           @Param("defaultFeed") String defaultFeed);
}
