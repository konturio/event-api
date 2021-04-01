package io.kontur.eventapi.noaatornado.job;

import io.kontur.eventapi.dao.DataLakeDao;
import io.kontur.eventapi.entity.DataLake;
import io.kontur.eventapi.job.AbstractJob;
import io.kontur.eventapi.noaatornado.client.NoaaTornadoClient;
import io.kontur.eventapi.noaatornado.parser.NoaaTornadoHTMLParser;
import io.kontur.eventapi.util.DateTimeUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static io.kontur.eventapi.util.CsvUtil.parseRow;

@Component
public class NoaaTornadoImportJob extends AbstractJob {

    private final static Logger LOG = LoggerFactory.getLogger(NoaaTornadoImportJob.class);
    public final static String NOAA_TORNADO_PROVIDER = "tornado.noaa";
    public final static String CSV_SEPARATOR = "\n";

    private final NoaaTornadoHTMLParser htmlParser;
    private final NoaaTornadoClient client;
    private final DataLakeDao dataLakeDao;

    public NoaaTornadoImportJob(MeterRegistry meterRegistry, NoaaTornadoHTMLParser htmlParser,
                                NoaaTornadoClient client, DataLakeDao dataLakeDao) {
        super(meterRegistry);
        this.htmlParser = htmlParser;
        this.client = client;
        this.dataLakeDao = dataLakeDao;
    }

    @Override
    public String getName() {
        return "noaaTornado";
    }

    @Override
    public void execute() throws Exception {
        Map<String, OffsetDateTime> filenamesAndUpdateDates = htmlParser.parseFilenamesAndUpdateDates();
        List<DataLake> allDataLakes = filenamesAndUpdateDates.entrySet().stream()
                .filter(entry -> isNewOrUpdatedFile(entry.getValue()))
                .map(entry -> processFile(entry.getKey(), entry.getValue()))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        dataLakeDao.storeDataLakes(allDataLakes);
    }

    private List<DataLake> processFile(String filename, OffsetDateTime updatedAt) {
        try {
            String csv = decompressGZIP(client.getGZIP(filename));
            String[] rows = StringUtils.split(csv, CSV_SEPARATOR);
            String header = rows[0];
            List<DataLake> dataLakes = new ArrayList<>();
            for (int i = 1; i < rows.length; i++) {
                processRow(header, rows[i], updatedAt).ifPresent(dataLakes::add);
            }
            return dataLakes;
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    private Optional<DataLake> processRow(String header, String row, OffsetDateTime updatedAt) {
        String externalId = parseRow(header, row).get("EVENT_ID");
        Optional<DataLake> existingDataLake = dataLakeDao.getDataLakeByExternalIdAndProvider(externalId, NOAA_TORNADO_PROVIDER);
        if (existingDataLake.isEmpty() || !existingDataLake.get().getUpdatedAt().isEqual(updatedAt)) {
            DataLake dataLake = new DataLake(UUID.randomUUID(), externalId, updatedAt, DateTimeUtil.uniqueOffsetDateTime());
            dataLake.setProvider(NOAA_TORNADO_PROVIDER);
            dataLake.setData(header + CSV_SEPARATOR + row);
            return Optional.of(dataLake);
        }
        return Optional.empty();
    }

    private boolean isNewOrUpdatedFile(OffsetDateTime fileUpdatedAt) {
        Optional<DataLake> latestUpdatedHazard = dataLakeDao.getLatestUpdatedHazard(NOAA_TORNADO_PROVIDER);
        return latestUpdatedHazard.isEmpty() || fileUpdatedAt.isAfter(latestUpdatedHazard.get().getUpdatedAt());
    }

    private String decompressGZIP(byte[] gzip) throws IOException{
        GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(gzip));
        return new String(gzipInputStream.readAllBytes());
    }
}
