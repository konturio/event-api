package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.DataLakeMapper;
import io.kontur.eventapi.entity.DataLake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

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

    public List<DataLake> getPdcEventsWithoutAreas() {
        return mapper.getPdcHazardsWithoutAreas();
    }

    public List<DataLake> getDenormalizedEvents() {
        return mapper.getDenormalizedEvents();
    }
}
