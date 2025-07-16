package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.NormalizedObservationsMapper;
import org.springframework.stereotype.Component;

@Component
public class ShakemapDao {
    private final NormalizedObservationsMapper mapper;

    public ShakemapDao(NormalizedObservationsMapper mapper) {
        this.mapper = mapper;
    }

    public String buildShakemapPolygons(String json) {
        return mapper.buildShakemapPolygons(json);
    }

    public String buildPgaMask(String json) {
        return mapper.buildPgaMask(json);
    }
}
