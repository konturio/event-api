package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.DataLake;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Optional;

@Mapper
public interface DataLakeMapper {

    void create(DataLake dataLake);

    Optional<DataLake> getLatestUpdatedEventForProvider(String provider);

    List<String> getPdcHazardsWithoutAreas();

    List<DataLake> getDenormalizedEvents();
}
