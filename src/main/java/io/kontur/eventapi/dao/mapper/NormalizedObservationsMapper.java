package io.kontur.eventapi.dao.mapper;

import io.kontur.eventapi.dto.NormalizedObservationsDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NormalizedObservationsMapper {

    int insert(NormalizedObservationsDto record);

}
