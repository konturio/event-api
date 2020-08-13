package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.KonturEventsMapper;
import io.kontur.eventapi.dto.KonturEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class KonturEventsDao {

    private final KonturEventsMapper mapper;

    @Autowired
    public KonturEventsDao(KonturEventsMapper mapper) {
        this.mapper = mapper;
    }

    public Optional<KonturEventDto> getLatestEventByExternalId(String externalId) {
        return mapper.getLatestEventByExternalId(externalId);
    }

    @Transactional
    public void insertEventVersion(List<KonturEventDto> events) {
        events.forEach(mapper::insert);
    }

}
