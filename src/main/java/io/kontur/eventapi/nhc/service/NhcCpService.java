package io.kontur.eventapi.nhc.service;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.nhc.client.NhcCpClient;
import io.kontur.eventapi.nhc.converter.NhcDataLakeConverter;
import io.kontur.eventapi.cap.service.CapImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NhcCpService extends CapImportService {

    @Autowired
    public NhcCpService(DataLakeDao dataLakeDao, NhcDataLakeConverter dataLakeConverter,
                        NhcCpClient nhcClient) {
        super(dataLakeDao, nhcClient, dataLakeConverter);
    }

}
