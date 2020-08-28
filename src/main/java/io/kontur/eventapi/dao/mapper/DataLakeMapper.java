package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.DataLake;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface DataLakeMapper {

    void create(DataLake dataLake);

    Optional<DataLake> getLatestUpdatedEventForProvider(String provider);

    List<DataLake> getPdcHazardsWithoutAreas();

    List<DataLake> getDenormalizedEvents();

    List<DataLake> getDataLakesByExternalId(String externalId);

    DataLake getDataLakeByObservationId(UUID observationId);
}
