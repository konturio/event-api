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

    public void saveGdacs(ParsedAlert alert, String provider){
        DataLake dataLake = dataLakeCoverter.convertGdacs(alert, provider);
        dataLakeDao.storeEventData(dataLake);
    }
}
