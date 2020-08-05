package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.dto.NormalizedRecordDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NormalizedRecordsMapper {

    int insertNormalizedRecord(NormalizedRecordDto record);

    List<String> getEventsIds();

    List<NormalizedRecordDto> getRecordsToCombine(String eventId);

}
