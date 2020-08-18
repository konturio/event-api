package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.dto.NormalizedObservationsDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.UUID;

@Mapper
public interface NormalizedObservationsMapper {

    int insert(NormalizedObservationsDto record);

    List<String> getExternalIdsToUpdate();

    List<NormalizedObservationsDto> getObservationsByExternalId(String externalId);

    List<NormalizedObservationsDto> getObservations(List<UUID> observationIds);
}
