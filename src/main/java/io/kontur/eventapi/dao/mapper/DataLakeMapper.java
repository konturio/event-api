package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.entity.ExposureGeohash;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Mapper
public interface DataLakeMapper {

    void create(DataLake dataLake);

    void markAsNormalized(@Param("observationId") UUID observationId);
    void markAsSkipped(@Param("observationId") UUID observationId);

    Optional<DataLake> getLatestUpdatedEventForProvider(@Param("provider") String provider);

    List<DataLake> getPdcHpSrvHazardsWithoutAreas();

    List<DataLake> getDenormalizedEvents(@Param("providers") List<String> providers);

    List<DataLake> getDataLakesByExternalId(@Param("externalId") String externalId);

    List<DataLake> getDataLakesByExternalIds(@Param("externalIds") Set<String> externalIds);

    List<DataLake> getDataLakesByExternalIdsAndProvider(@Param("externalIds") Set<String> externalIds,
                                                        @Param("provider") String provider);

    Set<String> getDataLakesIdByExternalIdsAndProvider(@Param("externalIds") Set<String> externalIds,
                                                       @Param("provider") String provider);

    Optional<DataLake> getLatestDataLakeByExternalIdAndProvider(@Param("externalId") String externalId,
                                                                @Param("provider") String provider);

    List<ExposureGeohash> getPdcExposureGeohashes(@Param("externalIds") Set<String> externalIds);

    Boolean isNewPdcExposure(@Param("externalId") String externalId,
                             @Param("geoHash") String geoHash);

    Boolean isNewEvent(@Param("externalId") String externalId,
                        @Param("provider") String provider,
                        @Param("updatedAt") String updatedAt);

    Integer getNotNormalizedObservationsCount();

    Integer getNotNormalizedObservationsCountForProviders(@Param("providers") List<String> providers);
}
