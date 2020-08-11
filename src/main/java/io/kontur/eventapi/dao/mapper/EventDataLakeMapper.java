package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.dto.EventDataLakeDto;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface EventDataLakeMapper {

    @Insert("INSERT INTO data_lake (observation_id, external_id, updated_at, loaded_at, provider, data) VALUES " +
            "(#{observationId}, #{externalId}, #{updatedAt}, #{loadedAt}, #{provider}, #{data})")
    void create(EventDataLakeDto eventDataLakeDto);

    @Select("SELECT observation_id as observationId, external_id as externalId, updated_at as updatedAt, loaded_at as loadedAt, provider, data " +
            "FROM data_lake WHERE provider = #{provider} ORDER BY updated_at DESC LIMIT 1")
    Optional<EventDataLakeDto> getLatestUpdatedEventForProvider(String provider);

    @Select("SELECT distinct e1.external_id " +
            "FROM data_lake e1 " +
            "WHERE e1.provider = 'hpSrvSearch' " +
            "  AND NOT EXISTS(select * from data_lake e2 where e1.external_id = e2.external_id and e2.provider = 'hpSrvMag')")
    List<String> getPdcHazardsWithoutAreas();

    @Select("SELECT observation_id as observationId, external_id as externalId, updated_at as updatedAt, loaded_at as loadedAt, provider, data " +
            "FROM data_lake e " +
            "WHERE NOT EXISTS(select * from normalized_observations nr where e.observation_id = nr.observation_id)")
    List<EventDataLakeDto> getDenormalizedEvents();
}
