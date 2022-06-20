package io.kontur.eventapi.nhc.service;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.nhc.client.NhcAtClient;
import io.kontur.eventapi.nhc.converter.NhcDataLakeConverter;
import io.kontur.eventapi.cap.service.CapImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NhcAtService extends CapImportService {

    @Autowired
    public NhcAtService(DataLakeDao dataLakeDao, NhcDataLakeConverter dataLakeConverter,
                        NhcAtClient nhcClient) {
        super(dataLakeDao, nhcClient, dataLakeConverter);
    }

}
