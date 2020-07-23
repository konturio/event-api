package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.dto.EventDataLakeDto;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface EventDataLakeMapper {

    @Insert("INSERT INTO event_data_lake (hazard_id, create_date, update_date, upload_date, provider, data) VALUES " +
            "(#{hazardId}, #{createDate}, #{updateDate}, #{uploadDate}, #{provider}, #{data})")
    void create(EventDataLakeDto eventDataLakeDto);

    @Select("SELECT hazard_id as hazardId, create_date as createDate, update_date as updateDate, upload_date as uploadDate, provider, data " +
            "FROM event_data_lake WHERE provider = #{provider} ORDER BY update_date DESC LIMIT 1")
    Optional<EventDataLakeDto> getLatestUpdatedHazard(String provider);
}
