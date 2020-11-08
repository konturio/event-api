package io.kontur.eventapi.gdacs.service;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.gdacs.converter.GdacsDataLakeConverter;
import io.kontur.eventapi.gdacs.dto.ParsedAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GdacsService {

    private final DataLakeDao dataLakeDao;
    private final GdacsDataLakeConverter dataLakeCoverter;

    @Autowired
    public GdacsService(DataLakeDao dataLakeDao, GdacsDataLakeConverter dataLakeCoverter) {
        this.dataLakeDao = dataLakeDao;
        this.dataLakeCoverter = dataLakeCoverter;
    }

    public void saveGdacs(ParsedAlert alert){
        DataLake dataLake = dataLakeCoverter.convertGdacs(alert);
        dataLakeDao.storeEventData(dataLake);
    }
    public void saveGdacsGeometry(ParsedAlert alert, String geometry){
        DataLake dataLake = dataLakeCoverter.convertGdacsWithGeometry(alert, geometry);
        dataLakeDao.storeEventData(dataLake);
    }
}
