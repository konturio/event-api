package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.dto.EventDataLakeDto;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface EventDataLakeMapper {

    @Insert("INSERT INTO event_data_lake (observation_id, hazard_id, create_date, update_date, upload_date, provider, data) VALUES " +
            "(#{observationId}, #{hazardId}, #{createDate}, #{updateDate}, #{uploadDate}, #{provider}, #{data})")
    void create(EventDataLakeDto eventDataLakeDto);

    @Select("SELECT observation_id as observationId, hazard_id as hazardId, create_date as createDate, update_date as updateDate, upload_date as uploadDate, provider, data " +
            "FROM event_data_lake WHERE provider = #{provider} ORDER BY update_date DESC LIMIT 1")
    Optional<EventDataLakeDto> getLatestUpdatedEventForProvider(String provider);

    @Select("SELECT distinct e1.hazard_id " +
            "FROM event_data_lake e1 " +
            "WHERE e1.provider = 'hpSrvSearch' " +
            "  AND NOT EXISTS(select * from event_data_lake e2 where e1.hazard_id = e2.hazard_id and e2.provider = 'hpSrvMag')")
    List<String> getPdcHazardsWithoutAreas();

    @Select("SELECT observation_id as observationId, hazard_id as hazardId, create_date as createDate, update_date as updateDate, upload_date as uploadDate, provider, data " +
            "FROM event_data_lake e " +
            "WHERE NOT EXISTS(select * from normalized_records nr where e.observation_id = nr.observation_id)")
    List<EventDataLakeDto> getDenormalizedEvents();
}
