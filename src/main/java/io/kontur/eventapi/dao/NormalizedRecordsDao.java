package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.NormalizedRecordsMapper;
import io.kontur.eventapi.dto.NormalizedRecordDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NormalizedRecordsDao {

    private final NormalizedRecordsMapper mapper;

    public NormalizedRecordsDao(NormalizedRecordsMapper mapper) {
        this.mapper = mapper;
    }

    public int insertNormalizedRecords(NormalizedRecordDto record) {
        return mapper.insertNormalizedRecord(record);
    }

    public List<String> getEventsIds() {
        return mapper.getEventsIds();
    }

    public List<NormalizedRecordDto> getRecordsToCombine(String eventId) {
        return mapper.getRecordsToCombine(eventId);
    }

}
