package io.kontur.eventapi.inciweb.service;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.inciweb.client.InciWebClient;
import io.kontur.eventapi.inciweb.converter.InciWebDataLakeConverter;
import io.kontur.eventapi.service.XmlImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InciWebService extends XmlImportService {

    @Autowired
    public InciWebService(DataLakeDao dataLakeDao, InciWebDataLakeConverter dataLakeConverter,
                          InciWebClient inciWebClient) {
        super(dataLakeDao, inciWebClient, dataLakeConverter);
    }
}
