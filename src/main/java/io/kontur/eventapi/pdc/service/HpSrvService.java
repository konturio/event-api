package io.kontur.eventapi.pdc.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.pdc.converter.PdcDataLakeConverter;
import org.springframework.stereotype.Component;

@Component
public class HpSrvService {

    private final DataLakeDao dataLakeDao;

    public HpSrvService(DataLakeDao dataLakeDao) {
        this.dataLakeDao = dataLakeDao;
    }

    public void saveHazard(JsonNode node) {
        DataLake dataLake = PdcDataLakeConverter.convertHpSrvHazardData(node);
        dataLakeDao.storeEventData(dataLake);
    }

    public void saveMag(String externalId, JsonNode json) {
        if (!json.isEmpty() && !json.get("features").isEmpty()) {
            DataLake magDto = PdcDataLakeConverter.convertHpSrvMagData(json, externalId);
            dataLakeDao.storeEventData(magDto);
        }
    }

}
