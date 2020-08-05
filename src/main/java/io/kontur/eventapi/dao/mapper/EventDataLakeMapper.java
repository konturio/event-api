package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.dto.EventDataLakeDto;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface EventDataLakeMapper {

    @Insert("INSERT INTO event_data_lake (observation_id, external_id, created_on, updated_on, loaded_on, provider, data) VALUES " +
            "(#{observationId}, #{externalId}, #{createdOn}, #{updatedOn}, #{loadedOn}, #{provider}, #{data})")
    void create(EventDataLakeDto eventDataLakeDto);

    @Select("SELECT observation_id as observationId, external_id as externalId, created_on as createdOn, updated_on as updatedOn, loaded_on as loadedOn, provider, data " +
            "FROM event_data_lake WHERE provider = #{provider} ORDER BY updated_on DESC LIMIT 1")
    Optional<EventDataLakeDto> getLatestUpdatedEventForProvider(String provider);

    @Select("SELECT distinct e1.external_id " +
            "FROM event_data_lake e1 " +
            "WHERE e1.provider = 'hpSrvSearch' " +
            "  AND NOT EXISTS(select * from event_data_lake e2 where e1.external_id = e2.external_id and e2.provider = 'hpSrvMag')")
    List<String> getPdcHazardsWithoutAreas();

    @Select("SELECT observation_id as observationId, external_id as externalId, created_on as createdOn, updated_on as updatedOn, loaded_on as loadedOn, provider, data " +
            "FROM event_data_lake e " +
            "WHERE NOT EXISTS(select * from normalized_records nr where e.observation_id = nr.observation_id)")
    List<EventDataLakeDto> getDenormalizedEvents();
}
