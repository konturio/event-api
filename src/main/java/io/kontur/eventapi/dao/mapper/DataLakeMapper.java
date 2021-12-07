package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.DataLake;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mapper
public interface DataLakeMapper {

    void create(DataLake dataLake);

    void markAsNormalized(@Param("observationId") UUID observationId);

    Optional<DataLake> getLatestUpdatedEventForProvider(@Param("provider") String provider);

    List<DataLake> getPdcHpSrvHazardsWithoutAreas();

    List<DataLake> getDenormalizedEvents();

    List<DataLake> getDataLakesByExternalId(@Param("externalId") String externalId);

    DataLake getDataLakeByObservationId(@Param("observationId") UUID observationId);

    Optional<DataLake> getLatestDataLakeByExternalIdAndProvider(@Param("externalId") String externalId,
                                                                @Param("provider") String provider);

    Boolean isNewPdcExposure(@Param("externalId") String externalId,
                             @Param("geoHash") String geoHash);

    Boolean isNewEvent(@Param("externalId") String externalId,
                        @Param("provider") String provider,
                        @Param("updatedAt") String updatedAt);

    Integer getNotNormalizedObservationsCount();
}
