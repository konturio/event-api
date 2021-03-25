package io.kontur.eventapi.tornado.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.tornado.client.NoaaTornadoClient;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.*;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import static io.kontur.eventapi.tornado.service.TornadoService.parseDateTimeWithPattern;
import static io.kontur.eventapi.util.CsvUtil.parseRow;

@Component
public class NoaaTornadoImportJob extends AbstractJob {

    private final static Logger LOG = LoggerFactory.getLogger(NoaaTornadoImportJob.class);
    private final NoaaTornadoClient noaaTornadoClient;
    private final DataLakeDao dataLakeDao;

    @Value("${noaaTornado.host}")
    private String URL;
    public final static String TORNADO_NOAA_PROVIDER = "tornado.noaa";

    protected NoaaTornadoImportJob(MeterRegistry meterRegistry, NoaaTornadoClient noaaTornadoClient, DataLakeDao dataLakeDao) {
        super(meterRegistry);
        this.noaaTornadoClient = noaaTornadoClient;
        this.dataLakeDao = dataLakeDao;
    }

    @Override
    public void execute() throws Exception {
        Map<String, OffsetDateTime> filenamesAndUpdateDates = getFilenamesAndUpdateDates();
        for (var filenameAndUpdateDate : filenamesAndUpdateDates.entrySet()) {
            try {
                byte[] gzip = noaaTornadoClient.getGZIP(filenameAndUpdateDate.getKey());
                String csv = decompressGZIP(gzip);
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

    private Map<String, OffsetDateTime> getFilenamesAndUpdateDates() {
        try {
            return Jsoup.connect(URL).get()
                    .select("tr:has(a:matches(StormEvents_details))")
                    .stream()
                    .map(element -> {
                        String[] rowItems = StringUtils.split(element.text());
                        String filename = rowItems[0];
                        String updateDate = StringUtils.joinWith(StringUtils.SPACE, rowItems[1], rowItems[2]);
                        return Map.entry(filename, parseDateTimeWithPattern(updateDate, "yyyy-MM-dd HH:mm"));
                    }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyMap();
    }

    private String decompressGZIP(byte[] gzip) throws IOException {
            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(gzip));
            return new String(gzipInputStream.readAllBytes());
    }

    private void createDataLakes(String csv, OffsetDateTime updatedAt) {
        List<DataLake> dataLakes = new ArrayList<>();
        String[] csvRows = csv.split("\n");
        String csvHeader = csvRows[0];

        for (int i = 1; i < csvRows.length; i++) {
            String externalId = parseRow(csvHeader, csvRows[i]).get("EVENT_ID");
            if (dataLakeDao.getDataLakeByExternalIdAndProvider(externalId, TORNADO_NOAA_PROVIDER).isEmpty()) {
                DataLake dataLake = new DataLake();
                dataLake.setObservationId(UUID.randomUUID());
                dataLake.setExternalId(externalId);
                dataLake.setLoadedAt(DateTimeUtil.uniqueOffsetDateTime());
                dataLake.setUpdatedAt(updatedAt);
                dataLake.setProvider(TORNADO_NOAA_PROVIDER);
                dataLake.setData(csvHeader + "\n" + csvRows[i]);
                dataLakes.add(dataLake);
            }
        }
        dataLakeDao.storeDataLakes(dataLakes);
    }
}
