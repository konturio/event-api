package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.NormalizedObservationsMapper;
import io.kontur.eventapi.dto.NormalizedObservationsDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class NormalizedObservationsDao {

    private final NormalizedObservationsMapper mapper;

    public NormalizedObservationsDao(NormalizedObservationsMapper mapper) {
        this.mapper = mapper;
    }

    public int insert(NormalizedObservationsDto record) {
        return mapper.insert(record);
    }

    public List<String> getExternalIdsToUpdate() {
        return mapper.getExternalIdsToUpdate();
    }

    public List<UUID> getObservationIdsByExternalId(String externalId) {
        return mapper.getObservationIdsByExternalId(externalId);
    }

}
