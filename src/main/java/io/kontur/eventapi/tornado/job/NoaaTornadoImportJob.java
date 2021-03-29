package io.kontur.eventapi.tornado.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.tornado.client.NoaaTornadoClient;
import io.kontur.eventapi.tornado.converter.TornadoDataLakeConverter;
import io.kontur.eventapi.tornado.service.NoaaTornadoImportService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.io.*;
import java.time.OffsetDateTime;
import java.util.*;
import static io.kontur.eventapi.util.CsvUtil.parseRow;

@Component
public class NoaaTornadoImportJob extends AbstractJob {

    private final static Logger LOG = LoggerFactory.getLogger(NoaaTornadoImportJob.class);

    public final static String TORNADO_NOAA_PROVIDER = "tornado.noaa";

    private final NoaaTornadoClient noaaTornadoClient;
    private final DataLakeDao dataLakeDao;
    private final TornadoDataLakeConverter tornadoDataLakeConverter;
    private final NoaaTornadoImportService noaaTornadoImportService;

    protected NoaaTornadoImportJob(MeterRegistry meterRegistry, NoaaTornadoClient noaaTornadoClient,
                                   DataLakeDao dataLakeDao, TornadoDataLakeConverter tornadoDataLakeConverter,
                                   NoaaTornadoImportService noaaTornadoImportService) {
        super(meterRegistry);
        this.noaaTornadoClient = noaaTornadoClient;
        this.dataLakeDao = dataLakeDao;
        this.tornadoDataLakeConverter = tornadoDataLakeConverter;
        this.noaaTornadoImportService = noaaTornadoImportService;
    }

    @Override
    public void execute() throws Exception {
        Map<String, OffsetDateTime> filenamesAndUpdateDates = noaaTornadoImportService.parseFilenamesAndUpdateDates();
        for (var filenameAndUpdateDate : filenamesAndUpdateDates.entrySet()) {
            try {
                byte[] gzip = noaaTornadoClient.getGZIP(filenameAndUpdateDate.getKey());
                String csv = noaaTornadoImportService.decompressGZIP(gzip);
                createDataLakes(csv, filenameAndUpdateDate.getValue());
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public String getName() {
        return "noaaTornadoImport";
    }

    private void createDataLakes(String csv, OffsetDateTime updatedAt) {
        List<DataLake> dataLakes = new ArrayList<>();
        String[] csvRows = csv.split("\n");
        String csvHeader = csvRows[0];

        for (int i = 1; i < csvRows.length; i++) {
            String externalId = parseRow(csvHeader, csvRows[i]).get("EVENT_ID");
            if (dataLakeDao.getDataLakeByExternalIdAndProvider(externalId, TORNADO_NOAA_PROVIDER).isEmpty()) {
                String data = csvHeader + "\n" + csvRows[i];
                DataLake dataLake = tornadoDataLakeConverter.convert(externalId, updatedAt, TORNADO_NOAA_PROVIDER, data);
                dataLakes.add(dataLake);
            }
        }
        dataLakeDao.storeDataLakes(dataLakes);
    }
}
