package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.MergePairMapper;
import io.kontur.eventapi.resource.dto.MergeCandidatePairDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MergePairDao {

    private final MergePairMapper mapper;

    @Autowired
    public MergePairDao(MergePairMapper mapper) {
        this.mapper = mapper;
    }

    public MergeCandidatePairDTO takeNextPair() {
        return mapper.takeNextPair();
    }

    public MergeCandidatePairDTO getPairByIds(String eventId1, String eventId2) {
        return mapper.getPairByIds(eventId1, eventId2);
    }

    public void markAsTaken(String eventId1, String eventId2) {
        mapper.markAsTaken(eventId1, eventId2);
    }
}
