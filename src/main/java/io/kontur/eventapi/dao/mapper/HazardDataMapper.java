package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.dto.HazardData;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HazardDataMapper {

    @Insert("INSERT INTO hazards_data (hazard_id, create_date, upload_date, provider, data) VALUES " +
            "(#{hazardId}, #{createDate}, #{uploadDate}, #{provider}, #{data})")
    void create(HazardData hazardData);


}
