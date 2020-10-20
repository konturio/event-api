package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.DataLakeMapper;
import io.kontur.eventapi.entity.DataLake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DataLakeDao {

    private final DataLakeMapper mapper;

    @Autowired
    public DataLakeDao(DataLakeMapper mapper) {
        this.mapper = mapper;
    }

    public void storeEventData(DataLake dataLake) {
        mapper.create(dataLake);
    }

    public List<DataLake> getDataLakesByExternalId(String externalId) {
        return mapper.getDataLakesByExternalId(externalId);
    }

    public Optional<DataLake> getLatestUpdatedHazard(String provider) {
        return mapper.getLatestUpdatedEventForProvider(provider);
    }

    public List<DataLake> getPdcHpSrvHazardsWithoutAreas() {
        return mapper.getPdcHpSrvHazardsWithoutAreas();
    }

    public List<DataLake> getDenormalizedEvents() {
        return mapper.getDenormalizedEvents();
    }

    public DataLake getDataLakeByObservationId(UUID observationId) {
        return mapper.getDataLakeByObservationId(observationId);
    }

    public List<DataLake> getDataLakeByExternalIdAndUpdateDate(String externalId, OffsetDateTime updatedAt){
        return mapper.getDataLakeByExternalIdAndUpdateDate(externalId,updatedAt);
    }
}
