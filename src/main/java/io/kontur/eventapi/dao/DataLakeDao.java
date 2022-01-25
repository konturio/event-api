package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.DataLakeMapper;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.ExposureGeohash;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class DataLakeDao {

    private final DataLakeMapper mapper;

    @Autowired
    public DataLakeDao(DataLakeMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public void storeDataLakes(List<DataLake> dataLakes) {
        dataLakes.forEach(this::storeEventData);
    }

    public void storeEventData(DataLake dataLake) {
        mapper.create(dataLake);
    }

    public void markAsNormalized(UUID observationId) {
        mapper.markAsNormalized(observationId);
    }

    public List<DataLake> getDataLakesByExternalId(String externalId) {
        return mapper.getDataLakesByExternalId(externalId);
    }

    public List<DataLake> getDataLakesByExternalIds(Set<String> externalIds) {
        return mapper.getDataLakesByExternalIds(externalIds);
    }

    public List<DataLake> getDataLakesByExternalIdsAndProvider(Set<String> externalIds, String provider) {
        return mapper.getDataLakesByExternalIdsAndProvider(externalIds, provider);
    }

    public Set<String> getDataLakesIdByExternalIdsAndProvider(Set<String> externalIds, String provider) {
        return mapper.getDataLakesIdByExternalIdsAndProvider(externalIds, provider);
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

    public Optional<DataLake> getLatestDataLakeByExternalIdAndProvider(String externalId, String provider) {
        return mapper.getLatestDataLakeByExternalIdAndProvider(externalId, provider);
    }

    public List<ExposureGeohash> getPdcExposureGeohashes(Set<String> externalIds) {
        return mapper.getPdcExposureGeohashes(externalIds);
    }

    public Boolean isNewPdcExposure(String externalId, String geoHash) {
        return mapper.isNewPdcExposure(externalId, geoHash);
    }

    public Boolean isNewEvent(String externalId, String provider, String updatedAt) {
        return mapper.isNewEvent(externalId, provider, updatedAt);
    }

    public Integer getNotNormalizedObservationsCount() {
        return mapper.getNotNormalizedObservationsCount();
    }

}
