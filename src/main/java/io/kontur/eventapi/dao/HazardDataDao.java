package io.kontur.eventapi.dao;

import io.kontur.eventapi.dao.mapper.HazardDataMapper;
import io.kontur.eventapi.dto.HazardData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HazardDataDao {

    private final HazardDataMapper mapper;

    @Autowired
    public HazardDataDao(HazardDataMapper mapper) {
        this.mapper = mapper;
    }

    public void storeHazardData(HazardData hazard) {
        mapper.create(hazard);
    }
}
