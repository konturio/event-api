package io.kontur.eventapi.gdacs.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.gdacs.converter.GdacsAlertXmlParser;
import io.kontur.eventapi.gdacs.service.GdacsService;
import io.kontur.eventapi.cap.job.CapImportJob;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GdacsHistoryImportJob extends CapImportJob {

    @Autowired
    public GdacsHistoryImportJob(GdacsService gdacsService, GdacsAlertXmlParser gdacsAlertParser,
                                 DataLakeDao dataLakeDao, MeterRegistry meterRegistry) {
        super(gdacsService, gdacsAlertParser, dataLakeDao, meterRegistry,
                null, null);
    }

    @Override
    public String getName() {
        return "gdacsHistoryImport";
    }

    @Override
    protected String getProvider() {
        return "GDACS";
    }
}
