package io.kontur.eventapi.gdacs.service;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.gdacs.converter.GdacsDataLakeCoverter;
import io.kontur.eventapi.gdacs.dto.AlertForInsertDataLake;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GdacsService {

    private final DataLakeDao dataLakeDao;
    private final GdacsDataLakeCoverter dataLakeCoverter;

    @Autowired
    public GdacsService(DataLakeDao dataLakeDao, GdacsDataLakeCoverter dataLakeCoverter) {
        this.dataLakeDao = dataLakeDao;
        this.dataLakeCoverter = dataLakeCoverter;
    }

    public void saveGdacs(AlertForInsertDataLake alert){
        DataLake dataLake = dataLakeCoverter.covertGdacs(alert);
        dataLakeDao.storeEventData(dataLake);
    }
}
