package io.kontur.eventapi.nhc.service;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.nhc.client.NhcEpClient;
import io.kontur.eventapi.nhc.converter.NhcDataLakeConverter;
import io.kontur.eventapi.cap.service.CapImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NhcEpService extends CapImportService {

    @Autowired
    public NhcEpService(DataLakeDao dataLakeDao, NhcDataLakeConverter dataLakeConverter,
                        NhcEpClient nhcClient) {
        super(dataLakeDao, nhcClient, dataLakeConverter);
    }
}
